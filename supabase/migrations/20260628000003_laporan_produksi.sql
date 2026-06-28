-- Migration: Laporan Produksi
-- Create Header Table
create table if not exists public.laporan_produksi (
    id uuid primary key default gen_random_uuid(),
    tanggal date not null,
    tipe_proses text not null,
    mp_direct integer not null default 0,
    mp_indirect integer not null default 0,
    jkn_hour integer not null default 0,
    jkn_menit integer not null default 0,
    ot_prod numeric not null default 0,
    ot_non numeric not null default 0,
    bantuan_keluar integer not null default 0,
    bantuan_masuk integer not null default 0,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null,
    unique(tanggal, tipe_proses)
);

-- Create Detail Table
create table if not exists public.laporan_produksi_detail (
    id uuid primary key default gen_random_uuid(),
    id_laporan uuid not null references public.laporan_produksi(id) on delete cascade,
    id_part uuid not null references public.m_part(id) on delete cascade,
    planning integer not null default 0,
    actual integer not null default 0,
    ng integer not null default 0,
    created_at timestamp with time zone default timezone('utc'::text, now()) not null
);

-- Enable RLS
alter table public.laporan_produksi enable row level security;
alter table public.laporan_produksi_detail enable row level security;

-- Policies
create policy "Allow public read access on laporan_produksi"
    on public.laporan_produksi for select using (true);
create policy "Allow public insert access on laporan_produksi"
    on public.laporan_produksi for insert with check (true);
create policy "Allow public update access on laporan_produksi"
    on public.laporan_produksi for update using (true);
create policy "Allow public delete access on laporan_produksi"
    on public.laporan_produksi for delete using (true);

create policy "Allow public read access on laporan_produksi_detail"
    on public.laporan_produksi_detail for select using (true);
create policy "Allow public insert access on laporan_produksi_detail"
    on public.laporan_produksi_detail for insert with check (true);
create policy "Allow public update access on laporan_produksi_detail"
    on public.laporan_produksi_detail for update using (true);
create policy "Allow public delete access on laporan_produksi_detail"
    on public.laporan_produksi_detail for delete using (true);

-- Grant privileges
grant select, insert, update, delete on public.laporan_produksi to anon, authenticated;
grant select, insert, update, delete on public.laporan_produksi_detail to anon, authenticated;
