TRUNCATE TABLE shard;

INSERT into SHARD (shard_id, shard_hash, shard_height, shard_state, zip_hash_crc, generator_ids, block_timeouts, block_timestamps) VALUES
(1, X'8dd2cb2fcd453c53b3fe53790ac1c104a6a31583e75972ff62bced9047a15176', 2, 99, null, '[]', '[]', '[]'),
(2, X'a3015d38155ea3fd95fe8952f579791e4ce7f5e1e21b4ca4e0c490553d94fb7d', 3, 100, X'a3015d38155ea3fd95fe8952f579791e4ce7f5e1e21b4ca4e0c490553d94fb7d',
    '[782179228250]', '[0, 1]', '[45673250, 45673251]'),
(3, X'931A8011F4BA1CDC0BCAE807032FE18B1E4F0B634F8DA6016E421D06C7E13693', 31, 100, null,
    '[782179228250, 4821792282200, 7821792282123976600]', '[1, 1]', '[45673251, 45673252]')
;