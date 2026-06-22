-- Menyamakan kontrak RPC checksheet dengan kolom enum pada schema vNext.

begin;

create or replace function public.rpc_submit_checksheet(payload jsonb)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
    sesi_id uuid;
    item_id uuid;
    defect_row_id uuid;
    item jsonb;
    defect_item jsonb;
    slot_item jsonb;
    total_diperiksa int;
    total_ok int;
    total_ng int;
    tipe text;
    tipe_enum public.tipe_proses_inspectra;
begin
    tipe := payload ->> 'tipeProses';

    if tipe not in ('PRESS', 'SEWING') then
        raise exception 'RPC ini hanya untuk Press dan Sewing.';
    end if;
    tipe_enum := tipe::public.tipe_proses_inspectra;

    total_diperiksa := coalesce(nullif(payload ->> 'totalDiperiksa', '')::int, 0);
    total_ok := coalesce(nullif(payload ->> 'totalOk', '')::int, 0);
    total_ng := coalesce(nullif(payload ->> 'totalNg', '')::int, 0);

    if total_diperiksa <= 0 then
        raise exception 'Jumlah pemeriksaan harus lebih besar dari nol.';
    end if;

    if total_diperiksa <> total_ok + total_ng then
        raise exception 'Total pemeriksaan tidak sesuai.';
    end if;

    insert into public.e_sesi_checksheet (
        kode_sesi,
        tipe_proses,
        tanggal_pemeriksaan,
        nama_shift,
        nama_operator,
        nama_line,
        device_id,
        app_version,
        total_diperiksa,
        total_ok,
        total_ng,
        rasio_ng_global,
        status
    )
    values (
        coalesce(
            payload ->> 'kodeSesi',
            'INS-' || extract(epoch from now())::bigint || '-' || substr(gen_random_uuid()::text, 1, 8)
        ),
        tipe_enum,
        coalesce(nullif(payload ->> 'tanggalPemeriksaan', '')::date, current_date),
        coalesce(nullif(payload ->> 'namaShift', ''), 'SHIFT_1'),
        nullif(payload ->> 'namaOperator', ''),
        nullif(payload ->> 'namaLine', ''),
        nullif(payload ->> 'deviceId', ''),
        nullif(payload ->> 'appVersion', ''),
        total_diperiksa,
        total_ok,
        total_ng,
        case when total_diperiksa > 0 then round(total_ng::numeric / total_diperiksa::numeric * 100, 3) else 0 end,
        'TERKIRIM'
    )
    returning id into sesi_id;

    for item in
        select * from jsonb_array_elements(coalesce(payload -> 'daftarPart', '[]'::jsonb))
    loop
        if not exists (
            select 1
            from public.m_part part
            where part.uniq_no = item ->> 'uniqNo'
              and part.aktif = true
              and part.komoditas = tipe_enum
        ) then
            raise exception 'Part % tidak sesuai dengan proses %.', item ->> 'uniqNo', tipe;
        end if;

        if coalesce(nullif(item ->> 'jumlahDiperiksa', '')::int, 0) > 0
           or coalesce(nullif(item ->> 'jumlahNg', '')::int, 0) > 0
        then
            insert into public.e_item_checksheet (
                id_sesi,
                uniq_no,
                jumlah_diperiksa,
                jumlah_ok,
                jumlah_ng,
                rasio_ng,
                catatan
            )
            values (
                sesi_id,
                item ->> 'uniqNo',
                coalesce(nullif(item ->> 'jumlahDiperiksa', '')::int, 0),
                coalesce(nullif(item ->> 'jumlahOk', '')::int, 0),
                coalesce(nullif(item ->> 'jumlahNg', '')::int, 0),
                coalesce(nullif(item ->> 'rasioNg', '')::numeric, 0),
                nullif(item ->> 'catatan', '')
            )
            returning id into item_id;

            for defect_item in
                select * from jsonb_array_elements(coalesce(item -> 'daftarDefectNg', '[]'::jsonb))
            loop
                if coalesce(nullif(defect_item ->> 'jumlahNg', '')::int, 0) > 0 then
                    insert into public.e_defect_checksheet (
                        id_item,
                        id_defect,
                        nama_defect_snapshot,
                        kategori,
                        jumlah
                    )
                    values (
                        item_id,
                        defect_item ->> 'idDefect',
                        coalesce(defect_item ->> 'namaDefect', '-'),
                        coalesce(defect_item ->> 'kategori', 'PROSES')::public.kategori_defect_inspectra,
                        coalesce(nullif(defect_item ->> 'jumlahNg', '')::int, 0)
                    )
                    returning id into defect_row_id;

                    for slot_item in
                        select * from jsonb_array_elements(coalesce(defect_item -> 'detailSlot', '[]'::jsonb))
                    loop
                        if coalesce(nullif(slot_item ->> 'jumlah', '')::int, 0) > 0 then
                            insert into public.e_defect_slot_checksheet (
                                id_defect_checksheet,
                                slot_waktu_id,
                                jumlah
                            )
                            values (
                                defect_row_id,
                                nullif(slot_item ->> 'slotId', '')::uuid,
                                coalesce(nullif(slot_item ->> 'jumlah', '')::int, 0)
                            );
                        end if;
                    end loop;
                end if;
            end loop;
        end if;
    end loop;

    if not exists (
        select 1 from public.e_item_checksheet where id_sesi = sesi_id
    ) then
        raise exception 'Isi minimal satu part sebelum mengirim lembar periksa.';
    end if;

    return sesi_id;
end;
$$;

grant execute on function public.rpc_submit_checksheet(jsonb) to anon, authenticated;

select pg_notify('pgrst', 'reload schema');

commit;
