-- Check if database exists, create if not
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'factory_incidents') THEN
    CREATE DATABASE factory_incidents;
    RAISE NOTICE 'Database factory_incidents created';
  ELSE
    RAISE NOTICE 'Database factory_incidents already exists';
  END IF;
END
$$;
