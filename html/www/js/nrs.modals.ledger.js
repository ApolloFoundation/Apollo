/******************************************************************************
 * Copyright © 2013-2016 The Nxt Core Developers.                             *
 * Copyright © 2016-2017 Jelurida IP B.V.                                     *
 *                                                                            *
 * See the LICENSE.txt file at the top-level directory of this distribution   *
 * for licensing information.                                                 *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,*
 * no part of the Nxt software, including this file, may be copied, modified, *
 * propagated, or distributed except according to the terms contained in the  *
 * LICENSE.txt file.                                                          *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

 /******************************************************************************
 * Copyright © 2017-2018 Apollo Foundation                                    *
 *                                                                            *
 ******************************************************************************/

/**
 * @depends {nrs.js}
 * @depends {nrs.modals.js}
 */
var NRS = (function(NRS, $) {
    var API = '/apl?';

    $("body").on("click", ".show_ledger_modal_action", function(event) {
		event.preventDefault();

        $('#get_date_private_transaction').attr('data-ledger', $(this).attr('data-entry'));
        NRS.modalsLedgerLoader = function() {
            console.log('loaded modals ledger');
        };
		if (NRS.fetchingModalData) {
			return;
		}
		NRS.fetchingModalData = true;
        var ledgerId, change, balance;
        if (typeof $(this).data("entry") == "object") {
            var dataObject = $(this).data("entry");
            ledgerId = dataObject["entry"];
            change = dataObject["change"];
            balance = dataObject["balance"];
        } else {
            ledgerId = $(this).data("entry");
            change = $(this).data("change");
            balance = $(this).data("balance");
        }
        if ($(this).data("back") == "true") {
            NRS.modalStack.pop(); // The forward modal
            NRS.modalStack.pop(); // The current modal
        }
        NRS.sendRequest("getAccountLedgerEntry+", { ledgerId: ledgerId }, function(response) {
			NRS.showLedgerEntryModal(response, change, balance);
		});
	});


	$('#get_date_private_transaction').submit(function() {
        var formParams = $( this ).serializeArray();

        var passphrase = formParams[0].value;

        if (NRS.validatePassphrase(passphrase)) {
            $('#incorrect_passphrase_message').removeClass('active');

            var url = API;

            url += 'requestType=getPrivateAccountLedgerEntry&';
            url += 'ledgerId=' +  $( this ).attr('data-ledger') + '&';
            url += 'secretPhrase=' + passphrase;

            $.get( url, function() {
            })
                .then(function (res) {
                    $('#get_private_transaction_type').modal('hide');
                    res = JSON.parse(res);


                    var detailsTable = $("#ledger_info_details_table");
                    detailsTable.find("tbody").empty().append(NRS.createInfoTable(res));
                    detailsTable.show();
                    $("#ledger_info_modal").modal("show");
                });

        } else {
            $('#incorrect_passphrase_message').addClass('active');
        }


    });


	NRS.showLedgerEntryModal = function(entry, change, balance) {
        try {
            NRS.setBackLink();
    		NRS.modalStack.push({ class: "show_ledger_modal_action", key: "entry", value: { entry: entry.ledgerId, change: change, balance: balance }});
            $("#ledger_info_modal_entry").html(entry.ledgerId);
            var entryDetails = $.extend({}, entry);
            try {
                entryDetails.eventType = $.t(entryDetails.eventType.toLowerCase());
                entryDetails.holdingType = $.t(entryDetails.holdingType.toLowerCase());

            } catch (e) {
                $('#get_date_modal').modal('show');

                return;
            }
            if (entryDetails.timestamp) {
                entryDetails.entryTime = NRS.formatTimestamp(entryDetails.timestamp);
            }
            if (entryDetails.holding) {
                entryDetails.holding_formatted_html = NRS.getTransactionLink(entry.holding);
                delete entryDetails.holding;
            }
            entryDetails.height_formatted_html = NRS.getBlockLink(entry.height);
            delete entryDetails.block;
            delete entryDetails.height;
            if (entryDetails.isTransactionEvent) {
                entryDetails.transaction_formatted_html = NRS.getTransactionLink(entry.event);
            }
            delete entryDetails.event;
            delete entryDetails.isTransactionEvent;
            entryDetails.change_formatted_html = change;
            delete entryDetails.change;
            entryDetails.balance_formatted_html = balance;
            delete entryDetails.balance;
            var detailsTable = $("#ledger_info_details_table");
            detailsTable.find("tbody").empty().append(NRS.createInfoTable(entryDetails));
            detailsTable.show();
            $("#ledger_info_modal").modal("show");
        } finally {
            NRS.fetchingModalData = false;
        }
	};

	return NRS;
}(NRS || {}, jQuery));