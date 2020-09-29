# How to create and install local MyRocks DB docker image

Link with instructions:
https://signal18.io/blog/docker-mariadb-myrocks

MariaDb in docker info - https://hub.docker.com/_/mariadb/

##  IMPORTANT !! You should have Docker to be installed locally first !!

### Linux

https://www.digitalocean.com/community/tutorials/how-to-install-and-use-docker-on-ubuntu-20-04-ru

#### 1. See prepared 'Dockerfile' script


#### 2. Run console commands

2.1 Download initial image. It we'll use as a base for a custom image and container :

$ docker pull mariadb:10.5.5


2.2 Build a new image 'mariadb:10.5' by running script 'Dockerfile' in the current folder :

$ docker build -t mariadb:10.5 .


2.3 One time start up a new container by using created image 'mariadb:10.5' and give it a name 'apl-mariadb':

$ docker run -p 3306:3306 \
    --name apl-mariadb \
    -e MYSQL_ROOT_PASSWORD=root \
    -e MYSQL_DATABASE=testdb \
    -e MYSQL_USER=testuser \
    -e MYSQL_PASSWORD=testpass \
    -d mariadb:10.5


2.4 Quick check it accessible on local PC :

$ mysql -h 127.0.0.1 -P3306 -u root -proot

2.6 Quick check by access with testuser on local PC :

$ mysql -h 127.0.0.1 -P3306 -u testuser -ptestpass


#### 3. Quickly check that MyRocks is enabled:

$ mysql -uroot -proot -h127.0.0.1 -P3306

or 

$ mysql -h 172.17.0.2 -u root -proot

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
 

#### Useful commands

Start container by name

$ docker start apl-mariadb

Stop container by name

$ docker stop apl-mariadb

Connect to bash in running container

$ docker exec -it apl-mariadb bash

See logs for running container 

$ docker logs apl-mariadb
