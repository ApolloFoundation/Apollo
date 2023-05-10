package com.apollocurrency.aplwallet.apl.core.dao;

public class MariaDBConfigs {

    public static final String DOCKER_IMAGE_NAME_VERSION = "mariadb:10.11";

    private static String[] envs;

    static {
        envs = new String[]{
            "--default-storage-engine=rocksdb",
            "--transaction-isolation=READ-COMMITTED",

            "--max_connections=10000",
            "--max_allowed_packet=16M",
            "--max_heap_table_size=16M",
            "--tmp_table_size=4M",
            "--key_buffer_size=2M",
            "--read_buffer_size=2M",
            "--sort_buffer_size=2M",
            "--table_open_cache=1048",
            "--table_open_cache_instances=3",

            "--thread_stack=256K",
            "--wait_timeout=200",
            "--table_open_cache_instances=1",
//
            "--rocksdb_compaction_sequential_deletes_count_sd=1",
            "--rocksdb_compaction_sequential_deletes=199999",
            "--rocksdb_compaction_sequential_deletes_window=200000",
////
////            "--rocksdb_max_open_files=300",
////            "--rocksdb_max_background_jobs=8",
////            "--rocksdb_table_cache_numshardbits=6",
            "--rocksdb_block_size=16384",
//            "--rocksdb_default_cf_options=write_buffer_size=256m;target_file_size_base=32m;max_bytes_for_level_base=512m;max_write_buffer_number=4;level0_file_num_compaction_trigger=4;level0_slowdown_writes_trigger=20;level0_stop_writes_trigger=30;max_write_buffer_number=4;block_based_table_factory={cache_index_and_filter_blocks=1;filter_policy=bloomfilter:10:false;whole_key_filtering=0};level_compaction_dynamic_level_bytes=true;optimize_filters_for_hits=true;memtable_prefix_bloom_size_ratio=0.05;prefix_extractor=capped:12;compaction_pri=kMinOverlappingRatio;compression=kLZ4Compression;bottommost_compression=kLZ4Compression;compression_opts=-14:4:0",
            "--rocksdb_max_subcompactions=4",
            "--rocksdb_compaction_readahead_size=16m",

            "--rocksdb_use_direct_reads=ON",
            "--rocksdb_use_direct_io_for_flush_and_compaction=ON"

//            don't work.
//            "--sql_log_bin=OFF"

        };
    }

    public static String[] getEnvs() {
        return envs;
    }
}
