echo "*********** BUILD Apollo **********"
echo "----------------------------------------"
echo "*********** BUILD apl-bom-ext **********"
cd apl-bom-ext && ./mvnw clean install

echo "*********** BUILD apollo-wallet (including apl-bom) **********"
cd .. && ./mvnw clean install $1