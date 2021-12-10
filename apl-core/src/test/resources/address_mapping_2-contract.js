class SimpleTest {
    field1 = '';
    field2 = 12345;
    field3 = new Error("msg");
}

class AddressMappingContract extends Contract {
    innerField = new SimpleTest();

    constructor(value, vendor) {
        console.log('--- in constructor ---', value, vendor);
        super();
        this.value = value;
        this.vendor = vendor;
        this.customer = '';
        this.paid = false;
        this.accepted = false;
        this.addresses = Mapping.address();
        this.addr1 = address("0xAA010203040506070809BB");
        this.addr3 = SMC.address(this.addr1);
    }

    set() {
        console.log('--- in set value=', msg.value(), typeof msg.value(), ', sender=', msg.sender(), ' self=', self);
        this.addresses.put("sender", msg.sender());
        this.addresses.put("self", self);
    }

    read() {
        console.log('--- in read value=', msg.value(), typeof msg.value(), ', sender=', msg.sender(), ' self=', self);
        console.log('--- in read [sender]=', this.addresses.get("sender"));
        console.log('--- in read [self]=', this.addresses.get("self"));
    }

}
