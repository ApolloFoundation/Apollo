FROM alpine:3.7

RUN \
  apk add --update go git make gcc musl-dev linux-headers ca-certificates && \
  apk add bash && \
  git clone --depth 1 https://github.com/ethereum/go-ethereum && \
  (cd go-ethereum && make geth) && \
  cp go-ethereum/build/bin/geth /geth && \
  apk del go git make gcc musl-dev linux-headers && \
  rm -rf /go-ethereum && rm -rf /var/cache/apk/* && \
  mkdir -p ~/.etherium/mychain/data/keystore

ADD ./devnet.sh /root/config/devnet.sh
ADD CustomGenesis.json /root/config/CustomGenesis.json
ADD keystore /root/.etherium/mychain/data/keystore

EXPOSE 8545
EXPOSE 8546
EXPOSE 30303

CMD ["/root/config/devnet.sh"]
