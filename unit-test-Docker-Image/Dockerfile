FROM mariadb:10.5.5

USER root

ARG DEBIAN_FRONTEND=noninteractive

RUN DEBIAN_FRONTEND=noninteractive;

RUN export DEBIAN_FRONTEND=noninteractive

# Prevent systemctl from being executed
RUN dpkg-divert --add /bin/systemctl && ln -sT /bin/true /bin/systemctl

RUN apt-get update

#RUN apt-get -y install sudo

#RUN sudo apt-get -y install sudo dialog apt-utils
RUN apt-get -y install sudo dialog apt-utils

#RUN sudo chmod -R u+rwx /etc/mysql/
#RUN chmod -R u+rwx /etc/mysql/

#RUN sudo chown -R mysql.mysql /etc/mysql/
#RUN chown -R mysql.mysql /etc/mysql/

#RUN sudo apt-get -y install mariadb-plugin-rocksdb
RUN apt-get -y install mariadb-plugin-rocksdb -f

#ADD my.cnf /etc/mysql/my.cnf
#COPY my.cnf /etc/mysql/my.cnf

#RUN sudo chmod -R u+rwx /etc/mysql/
#RUN chmod -R u+rwx /etc/mysql/

#RUN sudo chown -R mysql.mysql /etc/mysql/
#RUN chown -R mysql.mysql /etc/mysql/

ENV MYSQL_USER=testuser \
    MYSQL_PASSWORD=testpass \
    MYSQL_DATABASE=testdb \
    MYSQL_ROOT_PASSWORD=root

ADD . /

RUN rm -rf /var/cache/apt/lists/*
