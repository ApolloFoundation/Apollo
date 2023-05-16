DELETE FROM poll;

INSERT INTO poll
(db_id, id, account_id, name, description, options, min_num_options, max_num_options, min_range_value, max_range_value, TIMESTAMP, finish_height, voting_model, min_balance, min_balance_model, holding_id, height)
VALUES
(1, -1584246470026422303, 9211698109297098287, 'Apollo Moon', 'How far do you foresee Apollo reaching in the cryptocurrency market?', '["1st","2nd","3rd","4th","5th","Top 10","Top 20"]', 1, 1, 0, 1, 1080046, 8262, 0, null, 0, null, 1270),
(2, -7048492497119343629, -4417294517433192866, 'Mobil Platform', 'This only a test for the voting system.', '["IOS","Android"]', 1, 1, 0, 1, 1111646, 8816, 1, 3000000000000, 1, null, 1822);
