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

var NRS = (function(NRS, $, undefined) {
    var API = '/apl?';
    var dataBlock = null;
    NRS.modalBlocksLoader = function() {
        console.log('loaded modal blocks');
    };
	$("body").on("click", ".show_block_modal_action", function(event) {
		event.preventDefault();
		if (NRS.fetchingModalData) {
			return;
		}
		NRS.fetchingModalData = true;
        if ($(this).data("back") == "true") {
            NRS.modalStack.pop(); // The forward modal
            NRS.modalStack.pop(); // The current modal
        }
		var block = $(this).data("block");
        var isBlockId = $(this).data("id");
        var params = {
            "includeTransactions": "true",
            "includeExecutedPhased": "true"
        };
        if (isBlockId) {
            params["block"] = block;
        } else {
            params["height"] = block;
        }
        NRS.sendRequest("getBlock+", params, function(response) {
			NRS.showBlockModal(response);
		});
	});

    $('body').on('click', '[data-block]', function(){
        $('#show_private_transactions_enter_secret_passphrase').removeClass('active');
        dataBlock = $(this).attr('data-block');
    });

    $('body').on('click', '#height_show_private_transactions', function(){
        $('#show_private_transactions_enter_secret_passphrase').toggleClass('active');

    });


    $('body').on('click', '#show_private_transactions_enter_secret_passphrase_submit', function(){
        var val = $('#show_private_transactions_enter_secret_passphrase_value').val();

        var url = API;
        url += 'requestType=getPrivateBlockchainTransactions&';
        url += 'secretPhrase=' + val + '&';
        url += 'height=' + dataBlock;

        if (NRS.validatePassphrase(val, true)) {
            $('#incorrect_passphrase').removeClass('active');

            $.ajax({
                url: url,
                context: document.body
            }).done(function(data) {
                NRS.showBlockModal(JSON.parse(data));
            });
        } else {
            $('#incorrect_passphrase').addClass('active');
        }

    });

	NRS.showBlockModal = function(block) {
        NRS.setBackLink();
        NRS.modalStack.push({ class: "show_block_modal_action", key: "block", value: block.height });
        try {
            if (block.block) {
                $("#block_info_modal_block").html(NRS.escapeRespStr(block.block));
            }
            $("#block_info_transactions_tab_link").tab("show");

            var blockDetails = $.extend({}, block);
            delete blockDetails.transactions;
            blockDetails.generator_formatted_html = NRS.getAccountLink(blockDetails, "generator");
            delete blockDetails.generator;
            delete blockDetails.generatorRS;
            if (blockDetails.previousBlock) {
                blockDetails.previous_block_formatted_html = NRS.getBlockLink(blockDetails.height - 1, blockDetails.previousBlock);
                delete blockDetails.previousBlock;
            }
            if (blockDetails.nextBlock) {
                blockDetails.next_block_formatted_html = NRS.getBlockLink(blockDetails.height + 1, blockDetails.nextBlock);
                delete blockDetails.nextBlock;
            }
            if (blockDetails.timestamp) {
                blockDetails.blockGenerationTime = NRS.formatTimestamp(blockDetails.timestamp);
            }
            var detailsTable = $("#block_info_details_table");
            detailsTable.find("tbody").empty().append(NRS.createInfoTable(blockDetails));
            detailsTable.show();
            var transactionsTable = $("#block_info_transactions_table");
            if (block.transactions.length) {
                $("#height_show_private_transactions").show();
                $("#block_info_transactions_none").hide();
                transactionsTable.show();

                var rows = "";
                for (var i = 0; i < block.transactions.length; i++) {
                    var transaction = block.transactions[i];
                    if (transaction.amountATM) {
                        transaction.amount = new BigInteger(transaction.amountATM);
                        transaction.fee = new BigInteger(transaction.feeATM);
                        rows += "<tr>" +
                        "<td>" + transaction.transactionIndex + (transaction.phased ? "&nbsp<i class='fa fa-gavel' title='" + $.t("phased") + "'></i>" : "") + "</td>" +
                        "<td>" + NRS.getTransactionLink(transaction.transaction, NRS.formatTimestamp(transaction.timestamp)) + "</td>" +
                        "<td>" + NRS.getTransactionIconHTML(transaction.type, transaction.subtype) + "</td>" +
                        "<td>" + NRS.formatAmount(transaction.amount) + "</td>" +
                        "<td>" + NRS.formatAmount(transaction.fee) + "</td>" +
                        "<td>" + NRS.getAccountLink(transaction, "sender") + "</td>" +
                        "<td>" + NRS.getAccountLink(transaction, "recipient") + "</td>" +
                        "</tr>";
                    }
                }
                transactionsTable.find("tbody").empty().append(rows);
            } else {


                $("#height_show_private_transactions").hide();
                $("#block_info_transactions_none").show();
                transactionsTable.hide();
            }
            var executedPhasedTable = $("#block_info_executed_phased_table");
            if (block.executedPhasedTransactions && block.executedPhasedTransactions.length) {
                $("#block_info_executed_phased_none").hide();
                executedPhasedTable.show();
                rows = "";
                for (i = 0; i < block.executedPhasedTransactions.length; i++) {
                    transaction = block.executedPhasedTransactions[i];
                    rows += "<tr>" +
                        "<td>" + NRS.getTransactionLink(transaction.transaction, NRS.formatTimestamp(transaction.timestamp)) + "</td>" +
                        "<td>" + NRS.getTransactionIconHTML(transaction.type, transaction.subtype) + "</td>" +
                        "<td>" + NRS.getBlockLink(transaction.height) + "</td>" +
                        "<td>" + (transaction.attachment.phasingFinishHeight == block.height ? $.t("finished") : $.t("approved")) + "</td>";
                }
                executedPhasedTable.find("tbody").empty().append(rows);
            } else {
                $("#block_info_executed_phased_none").show();
                executedPhasedTable.hide();
            }
            var blockInfoModal = $('#block_info_modal');
            if (!blockInfoModal.data('bs.modal') || !blockInfoModal.data('bs.modal').isShown) {
                blockInfoModal.modal("show");
            }
        } finally {
            NRS.fetchingModalData = false;
        }
	};

	return NRS;
}(NRS || {}, jQuery));