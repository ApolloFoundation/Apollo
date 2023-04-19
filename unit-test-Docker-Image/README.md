# How to create and install local MyRocks DB docker image

Dockerfile file uses official script from repo
for building container's image in folder 
/unit-test-Docker-Image
Original script is :
https://github.com/docker-library/mariadb/blob/master/10.11/
and it is modified to our needs. 

MariaDb in docker info - https://hub.docker.com/_/mariadb/

##  IMPORTANT !! You should have Docker to be installed locally first !!

### Linux

https://www.digitalocean.com/community/tutorials/how-to-install-and-use-docker-on-ubuntu-20-04-ru

#### 1. Run console commands to build custom Apollo mariadb docker image

1.1 Go to sub folder to build a new image :

> cd unit-test-Docker-Image


1.2 Build a new image named 'mariadb:10.11' by running script 'Dockerfile' in the folder /unit-test-Docker-Image :

> docker build -t mariadb:10.11 .


1.3 CHECKING. Start up a new container by using created image 'mariadb:10.5' and give it a name 'apl-mariadb':

> docker run -p 3306:3306 \
    --name apl-mariadb \
    -e MYSQL_ROOT_HOST=% \
    -e MYSQL_ROOT_PASSWORD=rootpass \
    -e MYSQL_DATABASE=testdb \
    -e MYSQL_USER=testuser \
    -e MYSQL_PASSWORD=testpass \
    -d mariadb:10.11

You can stop reading here if your local docker image has been built successfully. You can try run 'slow' unit tests with mariadb in docker by using command:

> mvn test -Dgroups="slow" 

#### 2. Check connectivity with mariadb run docker container  

#### Check if docker run and run it if it's needed

> docker ps -a

| CONTAINER ID |     IMAGE     | COMMAND | CREATED | STATUS | PORTS | NAMES |
|:---|:-------------:|:---|:---:|:---:|:---:|:---:|
| da72e6287db9 | mariadb:10.11 | "docker-entrypoint.s…" | 14 hours ago | Up 1 second | 0.0.0.0:3306->3306/tcp | apl-mariadb |

See run container with NAME = apl-mariadb and STATUS = Up

You can start created container by command: $ docker start apl-mariadb 

2.1 Quick check mariadb is accessible on local PC :

> mysql -h 127.0.0.1 -P3306 -u root -prootpass

2.2 Quick check by access with 'testuser' on local PC :

> mysql -h 127.0.0.1 -P3306 -u testuser -ptestpass

2.3 You can look for all mariadb image optional run parameters by command:

> docker run -it --rm mariadb:10.11 --verbose --help

2.4 Quickly check that MyRocks is enabled:

> mysql -uroot -prootpass -h127.0.0.1 -P3306

or 

> mysql -h 172.17.0.2 -u root -prootpass

mysql> show engines;

| Engine | Support | Comment | Transactions | XA | Savepoints |
|:---|:---:|:---|:---:|:---:|:---:|
| ROCKSDB            | YES     | RocksDB storage engine                                                                          | YES          | YES  | YES        |
| MRG_MyISAM         | YES     | Collection of identical MyISAM tables                                                           | NO           | NO   | NO         |
| MEMORY             | YES     | Hash based, stored in memory, useful for temporary tables                                       | NO           | NO   | NO         |
| Aria               | YES     | Crash-safe tables with MyISAM heritage. Used for internal temporary tables and privilege tables | NO           | NO   | NO         |
| MyISAM             | YES     | Non-transactional engine with good performance and small data footprint                         | NO           | NO   | NO         |
| SEQUENCE           | YES     | Generated tables filled with sequential values                                                  | YES          | NO   | YES        |
| InnoDB             | DEFAULT | Supports transactions, row-level locking, foreign keys and encryption for tables                | YES          | YES  | YES        |
 

#### 3. Useful docker commands

3.1 Start container by name

> docker start apl-mariadb

3.2 Stop container by name

> docker stop apl-mariadb

3.3 Restart container

> docker restart apl-mariadb

3.4 Connect to MariaDb container bash

> docker exec -it apl-mariadb bash

3.5 See logs for running container (troubleshooting) 

> docker logs apl-mariadb

3.6 Find the IP address that has been assigned to the container:
    
> docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' apl-mariadb

3.7 Kill container

> docker kill apl-mariadb

3.8 Remove container (not a saved data ! )

> docker rm apl-mariadb

3.9 Remove data related to container

> docker rm -v apl-mariadb

##### 4. Useful SQL

select concat(user, '@', host, ' => ', json_detailed(priv)) from mysql.global_priv;

##### Links

MariaDB reserved keyword list - https://github.com/AnanthaRajuCprojects/Reserved-Key-Words-list-of-various-programming-languages/blob/master/MariaDB%20Reserved%20Words.md

MariaDB backup and restore databases in docker container :
https://blog.confirm.ch/backup-mysql-mariadb-docker-container/
https://stackoverflow.com/questions/26331651/how-can-i-backup-a-docker-container-with-its-data-volumes

