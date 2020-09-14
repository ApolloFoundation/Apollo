# How to create and install local MyRocks DB docker image

Link with instructions:
https://signal18.io/blog/docker-mariadb-myrocks


##  IMPORTANT !! You should have Docker to be installed locally first !!

1. See prepared 'Dockerfile' script


2. Run console commands

$ docker pull mariadb:10.4

$ docker build -t apl-myrocks .

$ docker run -d -p 3306:3306 --name apl-myrocks -e MYSQL_ROOT_PASSWORD=mypass myrocks

$ mysql -h 127.0.0.1 -P3306 -u root -pmypass


3. Quickly check that MyRocks is enabled:

$ mysql -uroot -pmypass -h127.0.0.1 -P3306

or 

$ mysql -h 172.17.0.2 -u root -pmypass

MariaDB [(none)]> show engines;
 
