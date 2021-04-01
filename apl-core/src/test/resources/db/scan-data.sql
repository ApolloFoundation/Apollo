DELETE FROM scan;

INSERT INTO scan
(RESCAN, VALIDATE  , HEIGHT                , SHUTDOWN              , CURRENT_HEIGHT              , PREPARATION_DONE) VALUES
(true   , false    , 1000                  , false                 , 1000                        , true)
;
