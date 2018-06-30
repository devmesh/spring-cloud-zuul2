# Spring Cloud Zuul2

Zuul integration with Spring Cloud.

## Getting Started

Start zuul server

```
./src/test/java/rocks/devmesh/spring/cloud/zuul/Application#main
```

Request to the '/healthcheck' endpoint

```
$ curl http://localhost:7001/healthcheck -v
*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 7001 (#0)
> GET /healthcheck HTTP/1.1
> Host: localhost:7001
> User-Agent: curl/7.54.0
> Accept: */*
>
< HTTP/1.1 200 OK
< Content-Length: 7
< X-Zuul-Filter-Executions: INBOUND-FILTERS_INBOUND_START-Filter[SUCCESS][0ms], sh.devmesh.spring.cloud.zuul.filters.Routes[SUCCESS][0ms], INBOUND-FILTERS_INBOUND_END-Filter[SUCCESS][0ms], sh.devmesh.spring.cloud.zuul.filters.Healthcheck[SUCCESS][0ms], OUTBOUND-FILTERS_OUTBOUND_START-Filter[SUCCESS][0ms]
< X-Zuul-Status: SUCCESS
< X-Zuul-Proxy-Attempts: []
< X-Zuul: zuul
< X-Originating-URL: http://localhost:7001/healthcheck
<
* Connection #0 to host localhost left intact
healthy%
```

## TODO

- Load filters from the spring context