ALTER TABLE `scan` ADD `shutdown` tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE `scan` ADD `current_height` int(11)  NOT NULL DEFAULT 0;
ALTER TABLE `scan` ADD `preparation_done` tinyint(1) NOT NULL DEFAULT 0;