-- Perbaikan lint untuk enum defect dan kolom detail Cutting.

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
begin
    tipe := payload ->> 'tipeProses';

    if tipe not in ('PRESS', 'SEWING') then
        raise exception 'RPC ini hanya untuk Press dan Sewing.';
    end if;

    total_diperiksa := coalesce(nullif(payload ->> 'totalDiperiksa', '')::int, 0);
    total_ok := coalesce(nullif(payload ->> 'totalOk', '')::int, 0);
    total_ng := coalesce(nullif(payload ->> 'totalNg', '')::int, 0);

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
        tipe::public.tipe_proses_inspectra,
        coalesce(nullif(payload ->> 'tanggalPemeriksaan', '')::date, current_date),
        coalesce(nullif(payload ->> 'namaShift', ''), 'SHIFT_1'),
        nullif(payload ->> 'namaOperator', ''),
        nullif(payload ->> 'namaLine', ''),
        nullif(payload ->> 'deviceId', ''),
        nullif(payload ->> 'appVersion', ''),
        total_diperiksa,
        total_ok,
        total_ng,
        coalesce(nullif(payload ->> 'rasioNgGlobal', '')::numeric, 0),
        'TERKIRIM'
    )
    returning id into sesi_id;

    for item in
        select * from jsonb_array_elements(coalesce(payload -> 'daftarPart', '[]'::jsonb))
    loop
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

    return sesi_id;
end;
$$;

grant execute on function public.rpc_submit_checksheet(jsonb) to anon, authenticated;

create or replace function public.rpc_submit_cutting_batch(payload jsonb)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
    sesi_id uuid;
    batch_id uuid;
    defect_item jsonb;
    total_layer int;
begin
    total_layer := coalesce(nullif(payload ->> 'total_diperiksa', '')::int, 0);

    -- Insert Sesi
    insert into public.e_sesi_checksheet (
        tipe_proses,
        tanggal_pemeriksaan,
        nama_shift,
        nama_operator,
        nama_line,
        total_diperiksa,
        total_ok,
        total_ng,
        rasio_ng_global,
        status
    )
    values (
        'CUTTING',
        coalesce(nullif(payload ->> 'tanggal_pemeriksaan', '')::date, current_date),
        coalesce(nullif(payload ->> 'nama_shift', ''), 'SHIFT_1'),
        nullif(payload ->> 'nama_operator', ''),
        nullif(payload ->> 'nama_line', ''),
        total_layer,
        coalesce(nullif(payload ->> 'total_ok', '')::int, 0),
        coalesce(nullif(payload ->> 'total_ng', '')::int, 0),
        coalesce(nullif(payload ->> 'rasio_ng_global', '')::numeric, 0),
        'TERKIRIM'
    )
    returning id into sesi_id;

    -- Insert Batch
    insert into public.e_cutting_batch (
        id_sesi,
        material_id,
        nama_material_snapshot,
        spec_material_snapshot,
        no_lot_roll,
        no_roll,
        size_cutting_cm,
        ukuran_cutting_cm,
        qty_layer_ok,
        qty_layer_ng,
        waste_panjang_cm,
        nama_operator,
        catatan
    )
    values (
        sesi_id,
        (payload ->> 'material_id')::uuid,
        payload ->> 'nama_material_snapshot',
        nullif(payload ->> 'spec_material_snapshot', ''),
        nullif(payload ->> 'no_lot_roll', ''),
        nullif(payload ->> 'no_roll', ''),
        coalesce(nullif(payload ->> 'size_cutting_cm', '')::numeric, 0),
        coalesce(nullif(payload ->> 'ukuran_cutting_cm', '')::numeric, 0),
        coalesce(nullif(payload ->> 'qty_layer_ok', '')::int, 0),
        coalesce(nullif(payload ->> 'qty_layer_ng', '')::int, 0),
        coalesce(nullif(payload ->> 'waste_panjang_cm', '')::numeric, 0),
        nullif(payload ->> 'nama_operator', ''),
        nullif(payload ->> 'catatan', '')
    )
    returning id into batch_id;

    -- Insert Defects
    for defect_item in
        select * from jsonb_array_elements(coalesce(payload -> 'daftar_defect', '[]'::jsonb))
    loop
        if coalesce(nullif(defect_item ->> 'jumlah_layer_terdampak', '')::int, 0) > 0 then
            insert into public.e_cutting_defect_detail (
                id_cutting_batch,
                id_defect,
                nama_defect_snapshot,
                slot_waktu_id,
                jumlah_layer,
                panjang_defect_cm
            )
            values (
                batch_id,
                defect_item ->> 'id_defect',
                coalesce(defect_item ->> 'nama_defect_snapshot', '-'),
                nullif(defect_item ->> 'slot_waktu_id', '')::uuid,
                coalesce(nullif(defect_item ->> 'jumlah_layer_terdampak', '')::int, 0),
                nullif(defect_item ->> 'panjang_defect_cm', '')::numeric
            );
        end if;
    end loop;

    return sesi_id;
end;
$$;

grant execute on function public.rpc_submit_cutting_batch(jsonb) to anon, authenticated;

select pg_notify('pgrst', 'reload schema');

commit;
