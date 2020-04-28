
echo "*********** BUILD apl-bom-ext **********"
cd apl-bom-ext && ./mvnw clean install

echo "*********** BUILD apollo-wallet **********"
cd .. && ./mvnw clean install

echo "*********** BUILD apl-bom **********"
cd apl-bom && ./mvnw clean install
