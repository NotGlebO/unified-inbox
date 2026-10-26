-- V3: Add history_id column to gmail_accounts for Gmail Push Notifications tracking.
ALTER TABLE public.gmail_accounts
    ADD COLUMN IF NOT EXISTS history_id character varying(255);
