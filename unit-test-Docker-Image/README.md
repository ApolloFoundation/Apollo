# How to create and install local MyRocks DB docker image

Link with instructions:
https://signal18.io/blog/docker-mariadb-myrocks


##  IMPORTANT !! You should have Docker to be installed locally first !!

#### 1. See prepared 'Dockerfile' script


#### 2. Run console commands

2.1 Download initial image we'll use as a base :

$ docker pull mariadb:10.4


2.2 Build a new image 'apl-mariadb-rocksdb:10.4' by running script 'Dockerfile' in the current folder :

# $ docker build -t apl-mariadb-rocksdb:10.4 .
$ docker build -t mariadb:10.4 .


2.3 One time start up a new container by using NEW image 'mariadb:10.4' and give it a name 'apl-mariadb-rocksdb':

$ docker run -p 3306:3306 \
    --name apl-mariadb-rocksdb \
    -e MYSQL_ROOT_PASSWORD=mypass \
    -e MYSQL_DATABASE=testdb \
    -e MYSQL_USER=testuser \
    -e MYSQL_PASSWORD=testpass \
    -d mariadb:10.4


2.4 Quick check it accessible on local PC :

$ mysql -h 127.0.0.1 -P3306 -u root -pmypass

2.5 Assign more priviliges to test user:

mysql> grant all on *.* to 'testuser'@'%' identified by 'testpass' with grant option;

2.6 Quick check by access with testuser on local PC :

$ mysql -h 127.0.0.1 -P3306 -u testuser -pmypass


#### 3. Quickly check that MyRocks is enabled:

$ mysql -uroot -pmypass -h127.0.0.1 -P3306

or 

$ mysql -h 172.17.0.2 -u root -pmypass

MariaDB [(none)]> show engines;

| Engine | Support | Comment | Transactions | XA | Savepoints |
|:---|:---:|:---|:---:|:---:|:---:|
| ROCKSDB            | YES     | RocksDB storage engine                                                                          | YES          | YES  | YES        |
| MRG_MyISAM         | YES     | Collection of identical MyISAM tables                                                           | NO           | NO   | NO         |
| MEMORY             | YES     | Hash based, stored in memory, useful for temporary tables                                       | NO           | NO   | NO         |
| Aria               | YES     | Crash-safe tables with MyISAM heritage. Used for internal temporary tables and privilege tables | NO           | NO   | NO         |
| MyISAM             | YES     | Non-transactional engine with good performance and small data footprint                         | NO           | NO   | NO         |
| SEQUENCE           | YES     | Generated tables filled with sequential values                                                  | YES          | NO   | YES        |
| InnoDB             | DEFAULT | Supports transactions, row-level locking, foreign keys and encryption for tables                | YES          | YES  | YES        |
 

#### Usefull commands

Connect to bash in running container

$ docker exec -it apl-mariadb-rocksdb bash