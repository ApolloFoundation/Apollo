ALTER TABLE `scan`
    ADD COLUMN IF NOT EXISTS `shutdown` tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE `scan`
    ADD COLUMN IF NOT EXISTS `current_height` int(11) NOT NULL DEFAULT 0;
ALTER TABLE `scan`
    ADD COLUMN IF NOT EXISTS `preparation_done` tinyint(1) NOT NULL DEFAULT 0;
