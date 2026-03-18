ALTER TABLE "routes" ADD COLUMN "price_day_pass" DECIMAL(8,2);
ALTER TABLE "routes" ADD COLUMN "price_multi_day" DECIMAL(8,2);
ALTER TABLE "routes" ADD COLUMN "price_permanent" DECIMAL(8,2);
ALTER TABLE "routes" ADD COLUMN "view_count" INTEGER NOT NULL DEFAULT 0;
ALTER TABLE "routes" ADD COLUMN "navigation_count" INTEGER NOT NULL DEFAULT 0;
