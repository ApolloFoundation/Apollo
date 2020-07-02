
To build package, please execute the following:

```
mvn clean package
```

To run swagger editor from DockerHub, please execute the following:

``` 
docker pull swaggerapi/swagger-editor
docker run -d -p 80:8080 swaggerapi/swagger-editor
```
