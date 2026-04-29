alter table bb_product
    add column if not exists attributes jsonb;

update bb_product
set attributes = '{}'::jsonb
where attributes is null;

alter table bb_product
    alter column attributes set default '{}'::jsonb;
