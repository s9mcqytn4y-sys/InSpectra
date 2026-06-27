-- File: 20260628000002_grant_view_app_bootstrap.sql
-- Description: Memberi akses baca pada v_app_bootstrap ke role anon dan authenticated

begin;

grant select on public.v_app_bootstrap to anon, authenticated;

commit;
