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
 */
var NRS = (function(NRS, $, undefined) {
    var API = '/apl?';

    NRS.allowUpdate = true;
	NRS.lastTransactions = "";
	NRS.unconfirmedTransactions = [];
	NRS.unconfirmedTransactionIds = "";
	NRS.unconfirmedTransactionsChange = true;

    NRS.transactionsLoader = function() {
        console.log('loaded transactions');
    };

    NRS.paginate = function(transactionType, target) {
    	var that = this;
        this.page = 1;
        this.transactionType = transactionType;
        this.isPrivate = false;
        this.passPhrase = '';
        this.items = null;
        this.target = target;
		this.itemsFiltration = $(this.target).parent().parent();
		this.filter = null;
		this.account = null;
		this.publicKey  = null;
		this.privateKey = null;
		this.sharedKey = null;
		this.serverKey = null;
		this.blockHeight = null;

		this.previewTransactions = null;

        $(this.target).parent().find('[data-transactions-pagination]').click(function(e) {

	        if ($(e.target).attr('data-navigate-page') === 'prev') {
                that.page = that.page - 1;
            } if ($(e.target).attr('data-navigate-page') === 'next') {
		        that.page = that.page + 1;
	        }

            that.getItems(that.page);

        });

        this.itemsFiltration.click(function(){});

        this.setKeys = function(passphrase) {
            this.privateKey = NRS.getPrivateKey(passphrase);
            this.publicKey  = NRS.getPublicKey(converters.stringToHexString(passphrase));

		};
        this.setFiltration = function(filter) {
            this.filter = filter;
            this.filter = parseInt(this.filter);

            this.getItems();
		};
        
        this.initPaginations = function() {
            var html = '';

			html += '<div data-navigate-page="prev" class="fa fa-arrow-circle-left" style="color: #337ab7; font-size: 20px;"></div>';
			html += '<div class="page-nav"></div>';
			html += '<div data-navigate-page="next" class="fa fa-arrow-circle-right" style="color: #337ab7; font-size: 20px;"></div>';

            $('[data-transactions-pagination]').html(html);
        };
        
        this.setPrivate = function(passphrase) {
            this.isPrivate = true;
            this.passPhrase = passphrase;
            this.getItems();
        };
        
        this.unsetPrivate = function() {
	        this.isPrivate  = false;
	        this.passPhrase = '';
	        this.getItems();
	
        };

        this.getPreviewTransactions = function () {
			var url = API;
			url += 'requestType=getBlockchainTransactions';
			url += '&firstIndex=0&lastIndex=9';
			url += '&account=' + NRS.account;

			var target = $('#dashboard_table tbody');
            if (target) {
                $.ajax({
                    url: url,
                    type: 'GET',
                    cache: false,
                    success: function(data) {
                        var rows = "";
                        data = JSON.parse(data).transactions;

                        for (var i = 0; i < data.length; i++) {
                            var transaction = data[i];

                            transaction.confirmed = true;
                            rows += NRS.getTransactionRowHTML(transaction, false, {amount: 0, fee: 0});
                        }
                        var $el = $("#dashboard_contents");

                        if ($el.length) {
                            $el.empty().append(rows);
                        } else {
                            $el = $("#dashboard_table");
                            $el.find("tbody").empty().append(rows);
                            $el.find('[data-toggle="tooltip"]').tooltip();
                        }
                    },
                    error: function(data) {
                        console.log('err: ', data);
                    }
                });
			}

        };

        this.getItems = function(page, place, isPreview) {
        	if (page) {
                this.page = page;
            }

            if (place) {
	            this.account = place;
            }

            var indicator = '';
            indicator += parseInt(this.page * 15 - 14) + ' &hellip; ' + parseInt(this.page * 15);
            $(this.target).parent().find('[data-transactions-pagination]').find('.page-nav').html(indicator);

            var url = API;

            if (place) {
	            url += 'account=' + place + '&';

            } else {
	            url += 'account=' + NRS.account + '&';
            }

	        if (this.isPrivate) {
                if (this.transactionType === 'getBlockchainTransactions' || this.transactionType === 'getPrivateBlockchainTransactions') {
                    url += 'requestType=getPrivateBlockchainTransactions&';
                    url += 'publicKey=' + this.publicKey + '&';
                }
                if (this.transactionType === 'getAccountLedger' || this.transactionType === 'getPrivateAccountLedger') {
                    url += 'requestType=getPrivateAccountLedger&';
                    url += 'publicKey=' + this.publicKey + '&';
				}
            } else {
                if (this.transactionType === 'getBlockchainTransactions' || this.transactionType === 'getPrivateBlockchainTransactions') {
                    url += 'requestType=getBlockchainTransactions&';
                }
                if (this.transactionType === 'getAccountLedger' || this.transactionType === 'getPrivateAccountLedger') {
                    url += 'requestType=getAccountLedger&';
                }
                if (this.transactionType === 'getBlocks') {
                    url += 'requestType=getBlocks&';
                }
            }

            if (this.filter >= 0) {
                url += 'type=' + this.filter + '&';
			}

            var nextPageUrl = url;
            var prevPageUrl = url;

            nextPageUrl += 'firstIndex=' + parseInt((this.page + 1) * 14 - 14) + '&';
            nextPageUrl += 'lastIndex='  + (this.page + 1) * 16 + '&';

			prevPageUrl += 'firstIndex=' + parseInt((this.page - 1) * 14 - 14) + '&';
			prevPageUrl += 'lastIndex='  + (this.page - 1) * 14 + '&';

            url += 'firstIndex=' + parseInt((this.page) * 14 - 14) + '&';
            url += 'lastIndex='  + (this.page) * 14 + '&';
	          var that = this;
            var $el = $("#" + NRS.currentPage + "_contents");
            $el = $el.selector;

            $.ajax({
                url: url,
                type: 'GET',
                cache: false,
                success: function(data) {
                    that.validatePagimation(prevPageUrl, 'prev');
                	that.validatePagimation(nextPageUrl, 'next');

                    var rows = "";
                    that.items = JSON.parse(data);
					that.serverKey = that.items.serverPublicKey;

                    if (that.transactionType === 'getBlockchainTransactions' || that.transactionType === 'getPrivateBlockchainTransactions') {
                        that.items = JSON.parse(data).transactions;

	                    if (that.items.length < 15 && that.page == 1) {
                            $(that.target).parent().find('[data-transactions-pagination]').find('.page-nav').addClass('disabled');
						} else {
                            $(that.target).parent().find('[data-transactions-pagination]').find('.page-nav').removeClass('disabled');
                        }

                        for (var i = 0; i < that.items.length; i++) {
                            var transaction = that.items[i];
                            transaction.confirmed = true;
                            rows += NRS.getTransactionRowHTML(transaction, false, {amount: 0, fee: 0});
                        }
                        if ($el === '#transactions_contents') {
                            NRS.dataLoaded(rows);
	                    }

                         NRS.addPhasingInfoToTransactionRows(that.items);
                     }
                     if (that.transactionType === 'getAccountLedger' || that.transactionType === 'getPrivateAccountLedger') {
                         that.items = JSON.parse(data).entries;

                         that.serverKey = JSON.parse(data).serverPublicKey;

                         if (that.items) {
                             var decimalParams = NRS.getLedgerNumberOfDecimals(that.items);
                             for (var i = 0; i < that.items.length; i++) {
                                 var entry = that.items[i];

                                 if ('encryptedLedgerEntry' in entry) {
                                     var options = {
                                         publicKey  :  converters.hexStringToInt8ByteArray(that.serverKey),
                                         privateKey :  converters.hexStringToInt8ByteArray(that.privateKey),
                                     };

                                     options.sharedKey = NRS.getSharedSecretJava(options.privateKey, options.publicKey);

                                     var decrypted =  NRS.decryptDataJava(entry.encryptedLedgerEntry, options);
                                     decrypted = decrypted.message;
                                     decrypted = converters.hexStringToString(decrypted);
                                     decrypted = decrypted.slice(0, decrypted.lastIndexOf('}') + 1);
                                     decrypted = JSON.parse(decrypted);

                                     entry = decrypted;
                                 }

                                 rows += NRS.getLedgerEntryRow(entry, decimalParams);
                             }
						            }

                         if ($el === '#ledger_contents') {
                             NRS.dataLoaded(rows);
                         }
                     }
                     if (that.transactionType === 'getBlocks') {
                         that.items = JSON.parse(data).blocks;

                         if ($el === '#blocks_contents') {
                             NRS.blocksPageLoaded(that.items);

                         }
                     }
                },
                error: function(data) {
                    console.log('err: ', data);
                }
            });
        };

        this.validatePagimation = function(url, action) {
        	if (action === 'prev') {
                $.ajax({
                    url: url,
                    type: 'GET',
                    cache: false,
                    success: function(data) {

                        var transactions = that.items = JSON.parse(data).transactions;
                        var entries = that.items = JSON.parse(data).entries;
                        var blocks = that.items = JSON.parse(data).blocks;

                        if (transactions) {
                            checkItems(transactions, 'prev');
						}
						if (entries) {
                            checkItems(entries, 'prev');
						}
						if (blocks) {
                            checkItems(blocks, 'prev')
						}
                    },
                    error: function(data) {
                        console.log('err: ', data);
                    }
                });
			}
			if (action === 'next') {
                $.ajax({
                    url: url,
                    type: 'GET',
                    cache: false,
                    success: function(data) {
                        var transactions = that.items = JSON.parse(data).transactions;
                        var entries = that.items = JSON.parse(data).entries;
                        var blocks = that.items = JSON.parse(data).blocks;

                        if (transactions) {
                            checkItems(transactions, 'next');
                        }
                        if (entries) {
                            checkItems(entries, 'next');
                        }
                        if (blocks) {
                            checkItems(blocks, 'next')
                        }
                    },
                    error: function(data) {
                        console.log('err: ', data);
                    }
                });
			}

			function checkItems(array, action) {
                if (array.length == 1 || array.length == 0) {
                    $(that.target).parent().find('[data-transactions-pagination]').find('[data-navigate-page="' + action + '"]').addClass('disabled');
                }
                else {
                    $(that.target).parent().find('[data-transactions-pagination]').find('[data-navigate-page="' + action + '"]').removeClass('disabled');
                }
			}
		};

        this.renderItems = function() {
            var indicator = '';

            indicator += parseInt(this.page * 15 - 14) + ' &hellip; ' + parseInt(this.page * 15);

            $(this.target).parent().find('[data-transactions-pagination]').find('.page-nav').html(indicator);


            this.getItems();
		};

		this.logPulling = function() {
			var that = this;
        	setInterval(function() {
                that.getItems();

            }, 60000)
		};
		this.blocksPulling = function() {
			var that = this;

			setInterval(function(){
                $.ajax({
                    url: API + 'requestType=getBlock',
                    type: 'GET',
                    cache: false,
                    success: function(data) {
                    	data  = JSON.parse(data);

                        $('.nrs_current_block').html(that.blockHeight);
                        $('#nrs_current_block_time').html(NRS.formatTimestamp(data.timestamp));

                    	if (that.blockHeight && that.blockHeight !== data.height) {
							that.blockHeight = data.height;

                            $('[data-block]').empty().html(that.blockHeight);
                            $('[data-block]').attr('data-block', that.blockHeight);

							that.getItems();
						}
						if (!that.blockHeight) {
							that.blockHeight = data.height;
                            $('[data-block]').empty().html(that.blockHeight);
                            $('[data-block]').attr('data-block', that.blockHeight);

                            that.getItems();
						}
                    },
                    error: function(data) {
                        console.log('err: ', data);
                    }
                });
                that.getPreviewTransactions();
			},2000);
		};
		this.destroyTable = function() {
			$('#transactions_table').find('tbody').empty();

			var $el = $("#" + NRS.currentPage + "_contents");
			$el = $el.selector;

			$.ajax({
				url: '/apl?account=' + NRS.account + '&requestType=getBlockchainTransactions&type=null&firstIndex=0&lastIndex=14&',
				type: 'GET',
				cache: false,
				success: function(data) {

					var rows = "";

					if (that.items.length < 15 && that.page == 1) {
						$(that.target).parent().find('[data-transactions-pagination]').find('.page-nav').addClass('disabled');
					} else {
						$(that.target).parent().find('[data-transactions-pagination]').find('.page-nav').removeClass('disabled');
					}

					for (var i = 0; i < that.items.length; i++) {
						var transaction = that.items[i];
						transaction.confirmed = true;
						rows += NRS.getTransactionRowHTML(transaction, false, {amount: 0, fee: 0});
					}

					if ($el === '#transactions_contents') {
						NRS.dataLoaded(rows);
					}

					NRS.addPhasingInfoToTransactionRows(that.items);

					$('#transactions_table').find('tbody').html(rows);

					if (that.transactionType === 'getBlockchainTransactions' || that.transactionType === 'getPrivateBlockchainTransactions') {
						that.items = JSON.parse(data).transactions;
					}
					if (that.transactionType === 'getAccountLedger' || that.transactionType === 'getPrivateAccountLedger') {
						that.items = JSON.parse(data).entries;

						var decimalParams = NRS.getLedgerNumberOfDecimals(that.items);
						for (var i = 0; i < that.items.length; i++) {
							var entry = that.items[i];
							rows += NRS.getLedgerEntryRow(entry, decimalParams);
						}

						if ($el === '#ledger_contents') {
							NRS.dataLoaded(rows);
						}
					}
					if (that.transactionType === 'getBlocks') {
						that.items = JSON.parse(data).blocks;

						if ($el === '#blocks_contents') {
							NRS.blocksPageLoaded(that.items);

						}
					}
				},
				error: function(data) {
					console.log('err: ', data);
				}
			});
		};

		// initialisation
        this.renderItems();
	    this.initPaginations();

        this.renderItems();
        this.logPulling();

        this.blocksPulling();
    };

    NRS.myTransactionPagination;
    NRS.accountLedgerPagination;
    NRS.blocksPagination;

    NRS.handleIncomingTransactions = function(transactions, confirmedTransactionIds) {
		var oldBlock = (confirmedTransactionIds === false); //we pass false instead of an [] in case there is no new block..

		if (typeof confirmedTransactionIds != "object") {
			confirmedTransactionIds = [];
		}

		if (confirmedTransactionIds.length) {
			NRS.lastTransactions = confirmedTransactionIds.toString();
		}

		if (confirmedTransactionIds.length || NRS.unconfirmedTransactionsChange) {
			transactions.sort(NRS.sortArray);
		}
		//Bug with popovers staying permanent when being open
		$('div.popover').hide();
		$('.td_transaction_phasing div.show_popover').popover('hide');

		//always refresh peers and unconfirmed transactions..
		if (NRS.currentPage == "peers") {
			NRS.incoming.peers();
		} else if (NRS.currentPage == "transactions"
            && $('#transactions_type_navi').find('li.active a').attr('data-transaction-type') == "unconfirmed") {
			NRS.incoming.transactions();
		} else {
			if (NRS.currentPage != 'messages' && (!oldBlock || NRS.unconfirmedTransactionsChange)) {
				if (NRS.incoming[NRS.currentPage]) {
					NRS.incoming[NRS.currentPage](transactions);
				}
			}
		}
		if (!oldBlock || NRS.unconfirmedTransactionsChange) {
			// always call incoming for messages to enable message notifications
			NRS.incoming['messages'](transactions);
			NRS.updateNotifications();
			NRS.setPhasingNotifications();
            NRS.setShufflingNotifications();
		}
	};

	NRS.getUnconfirmedTransactions = function(callback) {
		NRS.sendRequest("getUnconfirmedTransactions", {
			"account": NRS.account,
            "firstIndex": 0,
            "lastIndex": NRS.itemsPerPage
		}, function(response) {
			if (response.unconfirmedTransactions && response.unconfirmedTransactions.length) {
				var unconfirmedTransactions = [];
				var unconfirmedTransactionIds = [];

				response.unconfirmedTransactions.sort(function(x, y) {
					if (x.timestamp < y.timestamp) {
						return 1;
					} else if (x.timestamp > y.timestamp) {
						return -1;
					} else {
						return 0;
					}
				});

				for (var i = 0; i < response.unconfirmedTransactions.length; i++) {
					var unconfirmedTransaction = response.unconfirmedTransactions[i];
					unconfirmedTransaction.confirmed = false;
					unconfirmedTransaction.unconfirmed = true;
					unconfirmedTransaction.confirmations = "/";

					if (unconfirmedTransaction.attachment) {
						for (var key in unconfirmedTransaction.attachment) {
							if (!unconfirmedTransaction.attachment.hasOwnProperty(key)) {
								continue;
							}
							if (!unconfirmedTransaction.hasOwnProperty(key)) {
								unconfirmedTransaction[key] = unconfirmedTransaction.attachment[key];
							}
						}
					}
					unconfirmedTransactions.push(unconfirmedTransaction);
					unconfirmedTransactionIds.push(unconfirmedTransaction.transaction);
				}
				NRS.unconfirmedTransactions = unconfirmedTransactions;
				var unconfirmedTransactionIdString = unconfirmedTransactionIds.toString();
				if (unconfirmedTransactionIdString != NRS.unconfirmedTransactionIds) {
					NRS.unconfirmedTransactionsChange = true;
					NRS.setUnconfirmedNotifications();
					NRS.unconfirmedTransactionIds = unconfirmedTransactionIdString;
				} else {
					NRS.unconfirmedTransactionsChange = false;
				}

				if (callback) {
					callback(unconfirmedTransactions);
				}
			} else {
				NRS.unconfirmedTransactions = [];
				if (NRS.unconfirmedTransactionIds) {
					NRS.unconfirmedTransactionsChange = true;
					NRS.setUnconfirmedNotifications();
				} else {
					NRS.unconfirmedTransactionsChange = false;
				}

				NRS.unconfirmedTransactionIds = "";
				if (callback) {
					callback([]);
				}
			}
		});
	};

	NRS.getInitialTransactions = function(decimals) {

		NRS.myTransactionPagination = new NRS.paginate('getBlockchainTransactions', '#transactions_table');
		NRS.accountLedgerPagination = new NRS.paginate('getAccountLedger',          '#ledger_table');
		NRS.blocksPagination        = new NRS.paginate('getBlocks',                 '#blocks_table');


		if (NRS.myTransactionPagination && NRS.accountLedgerPagination && NRS.blocksPagination) {
			console.log('made switch ');

			NRS.myTransactionPagination.destroyTable();

			NRS.myTransactionPagination.unsetPrivate();
			NRS.accountLedgerPagination.unsetPrivate();
			NRS.blocksPagination       .unsetPrivate();

			NRS.myTransactionPagination.getItems(1, NRS.account);
			NRS.accountLedgerPagination.getItems(1, NRS.account);
			NRS.blocksPagination       .getItems(1, NRS.account);
		}

		NRS.sendRequest("getBlockchainTransactions", {
			"account": NRS.account,
			"firstIndex": 0,
			"lastIndex": 16
		}, function(response) {
			if (response.transactions && response.transactions.length) {
				var transactions = [];
				var transactionIds = [];

				for (var i = 0; i < response.transactions.length; i++) {
					var transaction = response.transactions[i];
					transaction.confirmed = true;
					transactions.push(transaction);
					transactionIds.push(transaction.transaction);
				}

				NRS.getUnconfirmedTransactions(function() {
					NRS.loadPage('dashboard');
				});
			} else {

				NRS.getUnconfirmedTransactions(function() {
					NRS.loadPage('dashboard');
				});
			}
		});
	};

	NRS.getNewTransactions = function() {
		//check if there is a new transaction..
		if (!NRS.blocks[0]) {
			return;
		}
        NRS.sendRequest("getBlockchainTransactions", {
			"account": NRS.account,
			"timestamp": Math.max(NRS.blocks[0].timestamp + 1, 0),
			"firstIndex": 0,
			"lastIndex": 0
		}, function(response) {
			//if there is, get latest 10 transactions
			if (response.transactions && response.transactions.length) {
				NRS.sendRequest("getBlockchainTransactions", {
					"account": NRS.account,
					"firstIndex": 0,
					"lastIndex": 9
				}, function(response) {
					if (response.transactions && response.transactions.length) {
						var transactionIds = [];

						$.each(response.transactions, function(key, transaction) {
							transactionIds.push(transaction.transaction);
							response.transactions[key].confirmed = true;
						});

						NRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
							NRS.handleIncomingTransactions(response.transactions.concat(unconfirmedTransactions), transactionIds);
						});
					} else {
						NRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
							NRS.handleIncomingTransactions(unconfirmedTransactions);
						});
					}
				});
			} else {
				NRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
					NRS.handleIncomingTransactions(unconfirmedTransactions);
				});
			}
		});
	};

	NRS.addUnconfirmedTransaction = function(transactionId, callback) {
		NRS.sendRequest("getTransaction", {
			"transaction": transactionId
		}, function(response) {
			if (!response.errorCode) {
				response.transaction = transactionId;
				response.confirmations = "/";
				response.confirmed = false;
				response.unconfirmed = true;

				if (response.attachment) {
					for (var key in response.attachment) {
                        if (!response.attachment.hasOwnProperty(key)) {
                            continue;
                        }
						if (!response.hasOwnProperty(key)) {
							response[key] = response.attachment[key];
						}
					}
				}
				var alreadyProcessed = false;
				try {
					var regex = new RegExp("(^|,)" + transactionId + "(,|$)");
					if (regex.exec(NRS.lastTransactions)) {
						alreadyProcessed = true;
					} else {
						$.each(NRS.unconfirmedTransactions, function(key, unconfirmedTransaction) {
							if (unconfirmedTransaction.transaction == transactionId) {
								alreadyProcessed = true;
								return false;
							}
						});
					}
				} catch (e) {
                    NRS.logConsole(e.message);
                }

				if (!alreadyProcessed) {
					NRS.unconfirmedTransactions.unshift(response);
				}
				if (callback) {
					callback(alreadyProcessed);
				}
				if (NRS.currentPage == 'transactions' || NRS.currentPage == 'dashboard') {
					$('div.popover').hide();
					$('.td_transaction_phasing div.show_popover').popover('hide');
					NRS.incoming[NRS.currentPage]();
				}

				NRS.getAccountInfo();
			} else if (callback) {
				callback(false);
			}
		});
	};

	NRS.sortArray = function(a, b) {
		return b.timestamp - a.timestamp;
	};

	NRS.getTransactionIconHTML = function(type, subtype) {
		var iconHTML = NRS.transactionTypes[type]['iconHTML'];
		var tooltip = $.t(NRS.transactionTypes[type].subTypes[subtype].i18nKeyTitle);
		return '<div class="bg-info" title="' + tooltip + '">' + iconHTML + '</div>';
	};

	NRS.addPhasedTransactionHTML = function(t) {
		var $tr = $('.tr_transaction_' + t.transaction + ':visible');
		var $tdPhasing = $tr.find('.td_transaction_phasing');
		var $approveBtn = $tr.find('.td_transaction_actions .approve_transaction_btn');

		if (t.attachment && t.attachment["version.Phasing"] && t.attachment.phasingVotingModel != undefined) {
			NRS.sendRequest("getPhasingPoll", {
				"transaction": t.transaction,
				"countVotes": true
			}, function(responsePoll) {
				if (responsePoll.transaction) {
					NRS.sendRequest("getPhasingPollVote", {
						"transaction": t.transaction,
						"account": NRS.accountRS
					}, function(responseVote) {
						var attachment = t.attachment;
						var vm = attachment.phasingVotingModel;
						var minBalance = parseFloat(attachment.phasingMinBalance);
						var mbModel = attachment.phasingMinBalanceModel;

						if ($approveBtn) {
							var disabled = false;
							var unconfirmedTransactions = NRS.unconfirmedTransactions;
							if (unconfirmedTransactions) {
								for (var i = 0; i < unconfirmedTransactions.length; i++) {
									var ut = unconfirmedTransactions[i];
									if (ut.attachment && ut.attachment["version.PhasingVoteCasting"] && ut.attachment.transactionFullHashes && ut.attachment.transactionFullHashes.length > 0) {
										if (ut.attachment.transactionFullHashes[0] == t.fullHash) {
											disabled = true;
											$approveBtn.attr('disabled', true);
										}
									}
								}
							}
							if (!disabled) {
								if (responseVote.transaction) {
									$approveBtn.attr('disabled', true);
								} else {
									$approveBtn.attr('disabled', false);
								}
							}
						}

						if (!responsePoll.result) {
							responsePoll.result = 0;
						}

						var state = "";
						var color = "";
						var icon = "";
						var minBalanceFormatted = "";
                        var finished = attachment.phasingFinishHeight <= NRS.lastBlockHeight;
						var finishHeightFormatted = String(attachment.phasingFinishHeight);
						var percentageFormatted = attachment.phasingQuorum > 0 ? NRS.calculatePercentage(responsePoll.result, attachment.phasingQuorum, 0) + "%" : "";
						var percentageProgressBar = attachment.phasingQuorum > 0 ? Math.round(responsePoll.result * 100 / attachment.phasingQuorum) : 0;
						var progressBarWidth = Math.round(percentageProgressBar / 2);
                        var approvedFormatted;
						if (responsePoll.approved || attachment.phasingQuorum == 0) {
							approvedFormatted = "Yes";
						} else {
							approvedFormatted = "No";
						}

						if (finished) {
							if (responsePoll.approved) {
								state = "success";
								color = "#00a65a";
							} else {
								state = "danger";
								color = "#f56954";
							}
						} else {
							state = "warning";
							color = "#f39c12";
						}

						var $popoverTable = $("<table class='table table-striped'></table>");
						var $popoverTypeTR = $("<tr><td></td><td></td></tr>");
						var $popoverVotesTR = $("<tr><td>" + $.t('votes', 'Votes') + ":</td><td></td></tr>");
						var $popoverPercentageTR = $("<tr><td>" + $.t('percentage', 'Percentage') + ":</td><td></td></tr>");
						var $popoverFinishTR = $("<tr><td>" + $.t('finish_height', 'Finish Height') + ":</td><td></td></tr>");
						var $popoverApprovedTR = $("<tr><td>" + $.t('approved', 'Approved') + ":</td><td></td></tr>");

						$popoverTypeTR.appendTo($popoverTable);
						$popoverVotesTR.appendTo($popoverTable);
						$popoverPercentageTR.appendTo($popoverTable);
						$popoverFinishTR.appendTo($popoverTable);
						$popoverApprovedTR.appendTo($popoverTable);

						$popoverPercentageTR.find("td:last").html(percentageFormatted);
						$popoverFinishTR.find("td:last").html(finishHeightFormatted);
						$popoverApprovedTR.find("td:last").html(approvedFormatted);

						var template = '<div class="popover" style="min-width:260px;"><div class="arrow"></div><div class="popover-inner">';
						template += '<h3 class="popover-title"></h3><div class="popover-content"><p></p></div></div></div>';

						var popoverConfig = {
							"html": true,
							"trigger": "hover",
							"placement": "top",
							"template": template
						};

						if (vm == -1) {
							icon = '<i class="fa ion-load-a"></i>';
						}
						if (vm == 0) {
							icon = '<i class="fa fa-group"></i>';
						}
						if (vm == 1) {
							icon = '<i class="fa fa-money"></i>';
						}
						if (vm == 2) {
							icon = '<i class="fa fa-signal"></i>';
						}
						if (vm == 3) {
							icon = '<i class="fa fa-bank"></i>';
						}
						if (vm == 4) {
							icon = '<i class="fa fa-thumbs-up"></i>';
						}
						if (vm == 5) {
							icon = '<i class="fa fa-question"></i>';
						}
						var phasingDiv = "";
						phasingDiv += '<div class="show_popover" style="display:inline-block;min-width:94px;text-align:left;border:1px solid #e2e2e2;background-color:#fff;padding:3px;" ';
	 				 	phasingDiv += 'data-toggle="popover" data-container="body">';
						phasingDiv += "<div class='label label-" + state + "' style='display:inline-block;margin-right:5px;'>" + icon + "</div>";

						if (vm == -1) {
							phasingDiv += '<span style="color:' + color + '">' + $.t("none") + '</span>';
						} else if (vm == 0) {
							phasingDiv += '<span style="color:' + color + '">' + String(responsePoll.result) + '</span> / <span>' + String(attachment.phasingQuorum) + '</span>';
						} else {
							phasingDiv += '<div class="progress" style="display:inline-block;height:10px;width: 50px;">';
	    					phasingDiv += '<div class="progress-bar progress-bar-' + state + '" role="progressbar" aria-valuenow="' + percentageProgressBar + '" ';
	    					phasingDiv += 'aria-valuemin="0" aria-valuemax="100" style="height:10px;width: ' + progressBarWidth + 'px;">';
	      					phasingDiv += '<span class="sr-only">' + percentageProgressBar + '% Complete</span>';
	    					phasingDiv += '</div>';
	  						phasingDiv += '</div> ';
	  					}
						phasingDiv += "</div>";
						var $phasingDiv = $(phasingDiv);
						popoverConfig["content"] = $popoverTable;
						$phasingDiv.popover(popoverConfig);
						$phasingDiv.appendTo($tdPhasing);
                        var votesFormatted;
						if (vm == 0) {
							$popoverTypeTR.find("td:first").html($.t('accounts', 'Accounts') + ":");
							$popoverTypeTR.find("td:last").html(String(attachment.phasingWhitelist ? attachment.phasingWhitelist.length : ""));
							votesFormatted = String(responsePoll.result) + " / " + String(attachment.phasingQuorum);
							$popoverVotesTR.find("td:last").html(votesFormatted);
						}
						if (vm == 1) {
							$popoverTypeTR.find("td:first").html($.t('accounts', 'Accounts') + ":");
							$popoverTypeTR.find("td:last").html(String(attachment.phasingWhitelist ? attachment.phasingWhitelist.length : ""));
							votesFormatted = NRS.convertToAPL(responsePoll.result) + " / " + NRS.convertToAPL(attachment.phasingQuorum) + " " + NRS.constants.COIN_SYMBOL;
							$popoverVotesTR.find("td:last").html(votesFormatted);
						}
						if (mbModel == 1) {
							if (minBalance > 0) {
								minBalanceFormatted = NRS.convertToAPL(minBalance) + " " + NRS.constants.COIN_SYMBOL;
								$approveBtn.data('minBalanceFormatted', minBalanceFormatted.escapeHTML());
							}
						}
						if (vm == 2 || mbModel == 2) {
							NRS.sendRequest("getAsset", {
								"asset": attachment.phasingHolding
							}, function(phResponse) {
								if (phResponse && phResponse.asset) {
									if (vm == 2) {
										$popoverTypeTR.find("td:first").html($.t('asset', 'Asset') + ":");
										$popoverTypeTR.find("td:last").html(String(phResponse.name));
										var votesFormatted = NRS.convertToATUf(responsePoll.result, phResponse.decimals) + " / ";
										votesFormatted += NRS.convertToATUf(attachment.phasingQuorum, phResponse.decimals) + " ATU";
										$popoverVotesTR.find("td:last").html(votesFormatted);
									}
									if (mbModel == 2) {
										if (minBalance > 0) {
											minBalanceFormatted = NRS.convertToATUf(minBalance, phResponse.decimals) + " ATU (" + phResponse.name + ")";
											$approveBtn.data('minBalanceFormatted', minBalanceFormatted.escapeHTML());
										}
									}
								}
							}, { isAsync: false });
						}
						if (vm == 3 || mbModel == 3) {
							NRS.sendRequest("getCurrency", {
								"currency": attachment.phasingHolding
							}, function(phResponse) {
								if (phResponse && phResponse.currency) {
									if (vm == 3) {
										$popoverTypeTR.find("td:first").html($.t('currency', 'Currency') + ":");
										$popoverTypeTR.find("td:last").html(String(phResponse.code));
										var votesFormatted = NRS.convertToATUf(responsePoll.result, phResponse.decimals) + " / ";
										votesFormatted += NRS.convertToATUf(attachment.phasingQuorum, phResponse.decimals) + " Units";
										$popoverVotesTR.find("td:last").html(votesFormatted);
									}
									if (mbModel == 3) {
										if (minBalance > 0) {
											minBalanceFormatted = NRS.convertToATUf(minBalance, phResponse.decimals) + " Units (" + phResponse.code + ")";
											$approveBtn.data('minBalanceFormatted', minBalanceFormatted.escapeHTML());
										}
									}
								}
							}, { isAsync: false });
						}
					});
				} else {
					$tdPhasing.html("&nbsp;");
				}
			}, { isAsync: false });
		} else {
			$tdPhasing.html("&nbsp;");
		}
	};

	NRS.addPhasingInfoToTransactionRows = function(transactions) {
		for (var i = 0; i < transactions.length; i++) {
			var transaction = transactions[i];
			NRS.addPhasedTransactionHTML(transaction);
		}
	};

    NRS.getTransactionRowHTML = function(t, actions, decimals, isScheduled) {
		if ('encryptedTransaction' in t) {
            var options = {

                publicKey  :  converters.hexStringToInt8ByteArray(NRS.myTransactionPagination.serverKey),
                privateKey :  converters.hexStringToInt8ByteArray(NRS.myTransactionPagination.privateKey),
            };


            options.sharedKey = NRS.getSharedSecretJava(options.privateKey, options.publicKey);


            var decrypted =  NRS.decryptDataJava(t.encryptedTransaction, options);
            decrypted = decrypted.message;
            decrypted = converters.hexStringToString(decrypted);
            decrypted = decrypted.slice(0, decrypted.lastIndexOf('}') + 1);
            decrypted = JSON.parse(decrypted);

            t = decrypted;
        }


        var transactionType = $.t(NRS.transactionTypes[t.type]['subTypes'][t.subtype]['i18nKeyTitle']);


        if (transactionType === 'Unknown') {


            transactionType = 'Private payment';
        }


        if (t.type == 1 && t.subtype == 6 && t.attachment.priceATM == "0") {
            if (t.sender == NRS.account && t.recipient == NRS.account) {
                transactionType = $.t("alias_sale_cancellation");
            } else {
                transactionType = $.t("alias_transfer");
            }
        }

        var amount = "";
        var sign = 0;
        var fee = new BigInteger(t.feeATM);
        var feeColor = "";
        var receiving = t.recipient == NRS.account && !(t.sender == NRS.account);
        if (receiving) {
            if (t.amountATM != "0") {
                amount = new BigInteger(t.amountATM);
                sign = 1;
            }
            feeColor = "color:black;";
        } else {
            if (t.sender != t.recipient) {
                if (t.amountATM != "0") {
                    amount = new BigInteger(t.amountATM);

                    amount = amount.negate();
                    sign = -1;
                }
            } else {
                if (t.amountATM != "0") {
                    amount = new BigInteger(t.amountATM); // send to myself
                }
            }
            feeColor = "color:red;";
        }
        var formattedAmount = "";
        if (amount != "") {
            formattedAmount = NRS.formatAmount(amount, false, false, decimals.amount);
        }
        var formattedFee = NRS.formatAmount(fee, false, false, decimals.fee);
        var amountColor = (sign == 1 ? "color:green;" : (sign == -1 ? "color:red;" : "color:black;"));
        var hasMessage = false;

        if (t.attachment) {
            if (t.attachment.encryptedMessage || t.attachment.message) {
                hasMessage = true;
            } else if (t.sender == NRS.account && t.attachment.encryptToSelfMessage) {
                hasMessage = true;
            }
        }
        var html = "";
        html += "<tr class='tr_transaction_" + t.transaction + "'>";
        html += "<td style='vertical-align:middle;'>";
        if (isScheduled) {
            html += "<a href='#' onclick='NRS.showTransactionModal(" + JSON.stringify(t) + ");'>" + NRS.formatTimestamp(t.timestamp) + "</a>";
        }  else {
            html += "<a class='show_transaction_modal_action' href='#' data-id='" + t.transaction + "' data-hash='" + t.fullHash + "' data-type='" + transactionType + "' data-timestamp='" + NRS.escapeRespStr(t.timestamp) + "' ";
            html += "data-transaction='" + NRS.escapeRespStr(t.transaction) + "'>";
            html += NRS.formatTimestamp(t.timestamp) + "</a>";
        }

        html += "</td>";
        html += "<td style='vertical-align:middle;text-align:center;'>" + (hasMessage ? "&nbsp; <i class='fa fa-envelope-o'></i>&nbsp;" : "&nbsp;") + "</td>";
        html += '<td style="vertical-align:middle;">';
        html += '<span class="label label-danger label-rouded">' + transactionType + '</span>';
        html += '</td>';
        html += "<td style='vertical-align:middle;text-align:right;" + amountColor + "'>" + formattedAmount + "</td>";
        html += "<td style='vertical-align:middle;text-align:right;" + feeColor + "'>" + formattedFee + "</td>";
        html += "<td style='vertical-align:middle;'>" + ((NRS.getAccountLink(t, "sender") == "/" && t.type == 2) ? "Asset Exchange" : NRS.getAccountLink(t, "sender")) + " ";
        html += "<i class='fa fa-arrow-circle-right' style='color:#777;'></i> " + ((NRS.getAccountLink(t, "recipient") == "/" && t.type == 2) ? "Asset Exchange" : NRS.getAccountLink(t, "recipient")) + "</td>";
        if (!isScheduled) {
            html += "<td class='td_transaction_phasing' style='min-width:100px;vertical-align:middle;text-align:center;'></td>";
            html += "<td style='vertical-align:middle;text-align:center;'>" + (t.confirmed ? NRS.getBlockLink(t.height, null, true) : "-") + "</td>";
            html += "<td class='confirmations' style='vertical-align:middle;text-align:center;font-size:12px;'>";
            html += "<span class='show_popover' data-content='" + (t.confirmed ? NRS.formatAmount(t.confirmations) + " " + $.t("confirmations") : $.t("unconfirmed_transaction")) + "' ";
            html += "data-container='body' data-placement='left'>";
            html += (!t.confirmed ? "-" : (t.confirmations > 1440 ? (NRS.formatAmount('144000000000') + "+") : NRS.formatAmount(t.confirmations))) + "</span></td>";
        }
        if (actions && actions.length != undefined) {
            html += '<td class="td_transaction_actions" style="vertical-align:middle;text-align:right;">';
            if (actions.indexOf('approve') > -1) {
                html += "<a class='btn btn-xs btn-default approve_transaction_btn' href='#' data-toggle='modal' data-target='#approve_transaction_modal' ";
                html += "data-transaction='" + NRS.escapeRespStr(t.transaction) + "' data-fullhash='" + NRS.escapeRespStr(t.fullHash) + "' ";
                html += "data-timestamp='" + t.timestamp + "' " + "data-votingmodel='" + t.attachment.phasingVotingModel + "' ";
                html += "data-fee='1' data-min-balance-formatted=''>" + $.t('approve') + "</a>";
            }
            if (actions.indexOf('delete') > -1) {
                html += "<a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#delete_scheduled_transaction_modal' ";
                html += "data-transaction='" + NRS.escapeRespStr(t.transaction) + "'>" + $.t("delete") + "</a>";
            }
            html += "</td>";
        }
        html += "</tr>";
        return html;
	};

    NRS.getLedgerEntryRow = function(entry, decimalParams) {
        var linkClass;
        var dataToken;
        if (entry.isTransactionEvent) {
            linkClass = "show_transaction_modal_action";
            dataToken = "data-transaction='" + NRS.escapeRespStr(entry.event) + "'";
        } else {
            linkClass = "show_block_modal_action";
            dataToken = "data-id='1' data-block='" + NRS.escapeRespStr(entry.event)+ "'";
        }
        var change = entry.change;
        var balance = entry.balance;
        var balanceType = "apl";
        var balanceEntity = "APL";
        var holdingIcon = "";
        if (change < 0) {
            change = String(change).substring(1);
        }
        if (/ASSET_BALANCE/i.test(entry.holdingType)) {
            NRS.sendRequest("getAsset", {"asset": entry.holding}, function (response) {
                balanceType = "asset";
                balanceEntity = response.name;
                change = NRS.formatQuantity(change, response.decimals, false, decimalParams.holdingChangeDecimals);
                balance = NRS.formatQuantity(balance, response.decimals, false, decimalParams.holdingBalanceDecimals);
                holdingIcon = "<i class='fa fa-signal'></i> ";
            }, { isAsync: false });
        } else if (/CURRENCY_BALANCE/i.test(entry.holdingType)) {
            NRS.sendRequest("getCurrency", {"currency": entry.holding}, function (response) {
                balanceType = "currency";
                balanceEntity = response.name;
                change = NRS.formatQuantity(change, response.decimals, false, decimalParams.holdingChangeDecimals);
                balance = NRS.formatQuantity(balance, response.decimals, false, decimalParams.holdingBalanceDecimals);
                holdingIcon =  "<i class='fa fa-bank'></i> ";
            }, { isAsync: false });
        } else {
            change = NRS.formatAmount(change, false, false, decimalParams.changeDecimals);
            balance = NRS.formatAmount(balance, false, false, decimalParams.balanceDecimals);
        }
        var sign = "";
		var color = "";
        if (entry.change > 0) {
			color = "color:green;";
		} else if (entry.change < 0) {
			color = "color:red;";
			sign = "-";
        }

        var eventType = NRS.escapeRespStr(entry.eventType);
        if (eventType.indexOf("ASSET") == 0 || eventType.indexOf("CURRENCY") == 0) {
            eventType = eventType.substring(eventType.indexOf("_") + 1);
        }

        if (eventType !== 'Private payment') {
            eventType = $.t(eventType.toLowerCase());
        }

        var html = "";
		html += "<tr>";
		html += "<td style='vertical-align:middle;'>";
  		html += "<a class='show_ledger_modal_action' href='#' data-entry='" + NRS.escapeRespStr(entry.ledgerId) +"'";
        html += "data-change='" + (entry.change < 0 ? ("-" + change) : change) + "' data-balance='" + balance + "'>";
  		html += NRS.formatTimestamp(entry.timestamp) + "</a>";
  		html += "</td>";
		html += '<td style="vertical-align:middle;">';
        html += '<span style="font-size:11px;display:inline-block;margin-top:5px;">' + eventType + '</span>';
        if (eventType === 'Private payment') {
            html += "<a class='" + linkClass + "' href='#' data-type='Private payment' data-id='" + entry.event + "' data-timestamp='" + NRS.escapeRespStr(entry.timestamp) + "' " + dataToken + ">";
        }
		else {
            html += "<a class='" + linkClass + "' href='#'";
            if (entry.eventType == 'TRANSACTION_FEE') {
                html += "data-type='Transaction fee'";
            }
			if (entry.eventType == 'ORDINARY_PAYMENT') {
                html += "data-type='Ordinary payment'";
			}
			if (entry.eventType == '"PRIVATE_PAYMENT"') {
                html += "data-type='Private payment'";
			}

            html += "data-timestamp='" + NRS.escapeRespStr(entry.timestamp) + "' " + dataToken + ">";
        }
        html += "<i class='fa fa-info'></i></a>";
		html += '</td>';
		if (balanceType == "apl") {
            html += "<td style='vertical-align:middle;" + color + "' class='numeric'>" + sign + change + "</td>";
            html += "<td style='vertical-align:middle;' class='numeric'>" + balance + "</td>";
            html += "<td></td>";
            html += "<td></td>";
            html += "<td></td>";
        } else {
            html += "<td></td>";
            html += "<td></td>";
            html += "<td>" + holdingIcon + balanceEntity + "</td>";
            html += "<td style='vertical-align:middle;" + color + "' class='numeric'>" + sign + change + "</td>";
            html += "<td style='vertical-align:middle;' class='numeric'>" + balance + "</td>";
        }
		return html;
	};

    $('#send_money_recipient').keyup(function(){
		$('#send_money_recipient_info').val($(this).val())
    });

    $('#send-money-amount').keyup(function() {
        $('#send_money_amount_info').val($(this).val())

    });

    $('#send_money_fee').keyup(function() {
        $('#send_money_fee_info').val($(this).val())

	});

	NRS.buildTransactionsTypeNavi = function() {
		var html = '';
		html += '<li role="presentation" class="active"><a href="#" data-transaction-type="" ';
		html += 'data-toggle="popover" data-placement="top" data-content="All" data-container="body" data-i18n="[data-content]all">';
		html += '<span data-i18n="all">All</span></a></li>';
        var typeNavi = $('#transactions_type_navi');
        typeNavi.append(html);

		$.each(NRS.transactionTypes, function(typeIndex, typeDict) {
			var titleString = $.t(typeDict.i18nKeyTitle);
			html = '<li role="presentation"><a href="#" data-transaction-type="' + typeIndex + '" ';
			html += 'data-toggle="popover" data-placement="top" data-content="' + titleString + '" data-container="body">';
			html += typeDict.iconHTML + '</a></li>';
			$('#transactions_type_navi').append(html);
		});

		html  = '<li role="presentation"><a href="#" data-transaction-type="unconfirmed" ';
		html += 'data-toggle="popover" data-placement="top" data-content="Unconfirmed (Account)" data-container="body" data-i18n="[data-content]unconfirmed_account">';
		html += '<i class="fa fa-circle-o"></i>&nbsp; <span data-i18n="unconfirmed">Unconfirmed</span></a></li>';
		typeNavi.append(html);

		html  = '<li role="presentation"><a href="#" data-transaction-type="phasing" ';
		html += 'data-toggle="popover" data-placement="top" data-content="Phasing (Pending)" data-container="body" data-i18n="[data-content]phasing_pending">';
		html += '<i class="fa fa-gavel"></i>&nbsp; <span data-i18n="phasing">Phasing</span></a></li>';
		typeNavi.append(html);

		html  = '<li role="presentation"><a href="#" data-transaction-type="all_unconfirmed" ';
		html += 'data-toggle="popover" data-placement="top" data-content="Unconfirmed (Everyone)" data-container="body" data-i18n="[data-content]unconfirmed_everyone">';
		html += '<i class="fa fa-circle-o"></i>&nbsp; <span data-i18n="all_unconfirmed">Unconfirmed (Everyone)</span></a></li>';
		typeNavi.append(html);

        typeNavi.find('a[data-toggle="popover"]').popover({
			"trigger": "hover"
		});
        typeNavi.find("[data-i18n]").i18n();
	};

	NRS.buildTransactionsSubTypeNavi = function() {
        var subtypeNavi = $('#transactions_sub_type_navi');
        subtypeNavi.empty();
		var html  = '<li role="presentation" class="active"><a href="#" data-transaction-sub-type="">';
		html += '<span>' + $.t("all_types") + '</span></a></li>';
		subtypeNavi.append(html);

		var typeIndex = $('#transactions_type_navi').find('li.active a').attr('data-transaction-type');

        NRS.myTransactionPagination.setFiltration(typeIndex);
	};

    NRS.displayUnconfirmedTransactions = function(account) {
        var params = {
            "firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
            "lastIndex": NRS.pageNumber * NRS.itemsPerPage
        };
        if (account != "") {
            params["account"] = account;
        }
        NRS.sendRequest("getUnconfirmedTransactions", params, function(response) {
			var rows = "";
			if (response.unconfirmedTransactions && response.unconfirmedTransactions.length) {
				if (NRS.allowUpdate) {
                    var decimals = NRS.getTransactionsAmountDecimals(response.unconfirmedTransactions);
                    for (var i = 0; i < response.unconfirmedTransactions.length; i++) {
                        rows += NRS.getTransactionRowHTML(response.unconfirmedTransactions[i], false, decimals);
                    }
				}

			}
			NRS.dataLoaded(rows);
		});
	};

	NRS.displayPhasedTransactions = function(transactions) {
		var params = {
			"account": NRS.account,
			"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
			"lastIndex": NRS.pageNumber * NRS.itemsPerPage
		};

        if (transactions) {
            // if (transactions && transactions.length) {
            //     var decimals = NRS.getTransactionsAmountDecimals(transactions);
            //     for (var i = 0; i < transactions.length; i++) {
            //         var transaction = transactions[i];
            //         transaction.confirmed = true;
            //         rows += NRS.getTransactionRowHTML(transaction, false, decimals);
            //     }
            //
            //     NRS.dataLoaded(rows);
            //     NRS.addPhasingInfoToTransactionRows(transactions);
            // } else {
            //     NRS.dataLoaded(rows);
            // }
        } else {
            // NRS.sendRequest("getBlockchainTransactions+", params, function(response) {
            //     if (response.transactions && response.transactions.length) {
            //         var decimals = NRS.getTransactionsAmountDecimals(response.transactions);
            //         for (var i = 0; i < response.transactions.length; i++) {
            //             var transaction = response.transactions[i];
            //             transaction.confirmed = true;
            //             rows += NRS.getTransactionRowHTML(transaction, false, decimals);
            //         }
            //
            //         NRS.dataLoaded(rows);
            //         NRS.addPhasingInfoToTransactionRows(response.transactions);
            //     } else {
            //         NRS.dataLoaded(rows);
            //     }
            // });
        }
	};

    NRS.pages.dashboard = function(transactions) {

        var rows = "";
        var params = {
            "account": NRS.account,
            "firstIndex": 0,
            "lastIndex": 9
        };
        var unconfirmedTransactions = NRS.unconfirmedTransactions;
		var decimals = NRS.getTransactionsAmountDecimals(unconfirmedTransactions);
        if (NRS.allowUpdate) {
            if (unconfirmedTransactions) {
                for (var i = 0; i < unconfirmedTransactions.length; i++) {
                    rows += NRS.getTransactionRowHTML(unconfirmedTransactions[i], false, decimals);
                }
            }
        }

        if (transactions) {
            NRS.allowUpdate = false;

            if (transactions && transactions.length) {
                var decimals = NRS.getTransactionsAmountDecimals(transactions);
                for (var i = 0; i < transactions.length; i++) {
                    var transaction = transactions[i];
                    transaction.confirmed = true;
                    rows += NRS.getTransactionRowHTML(transaction, false, decimals);
                }


                NRS.dataLoaded(rows);
                NRS.addPhasingInfoToTransactionRows(transactions);
            } else {
                NRS.dataLoaded(rows);
            }
        } else {
            if (NRS.allowUpdate) {
                NRS.sendRequest("getBlockchainTransactions+", params, function(response) {
                    if (response.transactions && response.transactions.length) {
                        var decimals = NRS.getTransactionsAmountDecimals(response.transactions);
                        for (var i = 0; i < response.transactions.length; i++) {
                            var transaction = response.transactions[i];
                            transaction.confirmed = true;
                            rows += NRS.getTransactionRowHTML(transaction, false, decimals);
                        }

                        NRS.dataLoaded(rows);
                        NRS.addPhasingInfoToTransactionRows(response.transactions);
                    } else {
                        NRS.dataLoaded(rows);
                    }
                });
			}
        }
    };

	NRS.incoming.dashboard = function() {
		NRS.loadPage("dashboard");
	};

	var isHoldingEntry = function (entry){
		return /ASSET_BALANCE/i.test(entry.holdingType) || /CURRENCY_BALANCE/i.test(entry.holdingType);
	};

    NRS.getLedgerNumberOfDecimals = function (entries){
		var decimalParams = {};
		decimalParams.changeDecimals = NRS.getNumberOfDecimals(entries, "change", function(entry) {
			if (isHoldingEntry(entry)) {
				return "";
			}
			return NRS.formatAmount(entry.change);
		});

		decimalParams.holdingChangeDecimals = NRS.getNumberOfDecimals(entries, "change", function(entry) {
            if (isHoldingEntry(entry)) {
					return NRS.formatQuantity(entry.change, entry.holdingInfo.decimals);
			}
			return "";
		});
		decimalParams.balanceDecimals = NRS.getNumberOfDecimals(entries, "balance", function(entry) {
			if (isHoldingEntry(entry)) {
				return "";
			}
			return NRS.formatAmount(entry.balance);
		});
		decimalParams.holdingBalanceDecimals = NRS.getNumberOfDecimals(entries, "balance", function(entry) {
			if (isHoldingEntry(entry)) {
					return NRS.formatQuantity(entry.balance, entry.holdingInfo.decimals);
			}
			return "";
		});
		return decimalParams;
	};

    NRS.pages.ledger = function(transactionds) {
		var rows = "";
        var params = {
            "account": NRS.account,
            "includeHoldingInfo": true,
            "firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
            "lastIndex": NRS.pageNumber * NRS.itemsPerPage
        };

        if (transactionds) {
            if (transactionds.entries && transactionds.entries.length) {
                if (transactionds.entries.length > transactionds.itemsPerPage) {
                    NRS.hasMorePages = true;
                    transactionds.entries.pop();
                }
                var decimalParams = NRS.getLedgerNumberOfDecimals(transactionds.entries);
                for (var i = 0; i < transactionds.entries.length; i++) {
                    var entry = transactionds.entries[i];
                    rows += NRS.getLedgerEntryRow(entry, decimalParams);
                }
            }
            NRS.dataLoaded(rows);
            if (NRS.ledgerTrimKeep > 0) {
                var ledgerMessage = $("#account_ledger_message");
                ledgerMessage.text($.t("account_ledger_message", { blocks: NRS.ledgerTrimKeep }));
                ledgerMessage.show();
            }
		} else {
            NRS.sendRequest("getAccountLedger+", params, function(response) {
            	$('.pagination-body').css('display', 'none');
                if (response.entries && response.entries.length) {
                    if (response.entries.length > NRS.itemsPerPage) {
                        NRS.hasMorePages = true;
                        response.entries.pop();
                    }
                    var decimalParams = NRS.getLedgerNumberOfDecimals(response.entries);
                    for (var i = 0; i < response.entries.length; i++) {
                        var entry = response.entries[i];
                        rows += NRS.getLedgerEntryRow(entry, decimalParams);
                    }
                }
                NRS.dataLoaded(rows);
                if (NRS.ledgerTrimKeep > 0) {
                    var ledgerMessage = $("#account_ledger_message");
                    ledgerMessage.text($.t("account_ledger_message", { blocks: NRS.ledgerTrimKeep }));
                    ledgerMessage.show();
                }
            });
		}
	};

	NRS.pages.transactions = function(callback, subpage) {
        var typeNavi = $('#transactions_type_navi');
        if (typeNavi.children().length == 0) {
			NRS.buildTransactionsTypeNavi();
			NRS.buildTransactionsSubTypeNavi();
		}

		if (subpage) {
			typeNavi.find('li a[data-transaction-type="' + subpage + '"]').click();
			return;
		}

		var selectedType = typeNavi.find('li.active a').attr('data-transaction-type');
		var selectedSubType = $('#transactions_sub_type_navi').find('li.active a').attr('data-transaction-sub-type');
		if (!selectedSubType) {
			selectedSubType = "";
		}
		if (selectedType == "unconfirmed") {
			NRS.displayUnconfirmedTransactions(NRS.account);
			return;
		}
		if (selectedType == "phasing") {
			NRS.displayPhasedTransactions();
			return;
		}
		if (selectedType == "all_unconfirmed") {
			NRS.displayUnconfirmedTransactions("");
			return;
		}

		var rows = "";
		var params = {
			"account": NRS.account,
			"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
			"lastIndex": NRS.pageNumber * NRS.itemsPerPage
		};
        var unconfirmedTransactions;
		if (selectedType) {
			params.type = selectedType;
			params.subtype = selectedSubType;
			unconfirmedTransactions = NRS.getUnconfirmedTransactionsFromCache(params.type, (params.subtype ? params.subtype : []));
		} else {
			unconfirmedTransactions = NRS.unconfirmedTransactions;
		}
		var decimals = NRS.getTransactionsAmountDecimals(unconfirmedTransactions);
		if (unconfirmedTransactions) {
			for (var i = 0; i < unconfirmedTransactions.length; i++) {
				rows += NRS.getTransactionRowHTML(unconfirmedTransactions[i], false, decimals);
			}
		}
		if (NRS.allowUpdate) {
            NRS.sendRequest("getBlockchainTransactions+", params, function(response) {
                if (response.transactions && response.transactions.length) {
                    if (response.transactions.length > NRS.itemsPerPage) {
                        NRS.hasMorePages = true;
                        response.transactions.pop();
                    }
                    var decimals = NRS.getTransactionsAmountDecimals(response.transactions);
                    for (var i = 0; i < response.transactions.length; i++) {
                        var transaction = response.transactions[i];
                        transaction.confirmed = true;
                        rows += NRS.getTransactionRowHTML(transaction, false, decimals);
                    }

                    NRS.dataLoaded(rows);
                    NRS.addPhasingInfoToTransactionRows(response.transactions);
                } else {
                    NRS.dataLoaded(rows);
                }
            });
		}
	};

	NRS.updateApprovalRequests = function() {
		var params = {
			"account": NRS.account,
			"firstIndex": 0,
			"lastIndex": 20
		};
		NRS.sendRequest("getVoterPhasedTransactions", params, function(response) {
			var $badge = $('#dashboard_link').find('.sm_treeview_submenu a[data-page="approval_requests_account"] span.badge');
			if (response.transactions && response.transactions.length) {
				if (response.transactions.length == 0) {
					$badge.hide();
				} else {
                    var length;
					if (response.transactions.length == 21) {
						length = "20+";
					} else {
						length = String(response.transactions.length);
					}
					$badge.text(length);
					$badge.show();
				}
			} else {
				$badge.hide();
			}
		});
		if (NRS.currentPage == 'approval_requests_account') {
			NRS.loadPage(NRS.currentPage);
		}
	};

	NRS.pages.approval_requests_account = function() {
		var params = {
			"account": NRS.account,
			"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
			"lastIndex": NRS.pageNumber * NRS.itemsPerPage
		};
		NRS.sendRequest("getVoterPhasedTransactions", params, function(response) {
			var rows = "";

			if (response.transactions && response.transactions.length) {
				if (response.transactions.length > NRS.itemsPerPage) {
					NRS.hasMorePages = true;
					response.transactions.pop();
				}
				var decimals = NRS.getTransactionsAmountDecimals(response.transactions);
				for (var i = 0; i < response.transactions.length; i++) {
					var t = response.transactions[i];
					t.confirmed = true;
					rows += NRS.getTransactionRowHTML(t, ['approve'], decimals);
				}
			}
			NRS.dataLoaded(rows);
			NRS.addPhasingInfoToTransactionRows(response.transactions);
		});
	};

    NRS.pages.scheduled_transactions = function(callback, subpage) {
    	
    	console.log('scheduled_transactions');
    	
     
    };

    $("#delete_scheduled_transaction_modal").on("show.bs.modal", function(e) {
        var $invoker = $(e.relatedTarget);
        var transaction = $invoker.data("transaction");
		$("#delete_scheduled_transaction_id").val(transaction);
    });

    NRS.forms.deleteScheduledTransaction = function($modal) {
    	var data = NRS.getFormData($modal.find("form:first"));
    	data.adminPassword = NRS.getAdminPassword();
		return { data: data };
    };

    NRS.forms.deleteScheduledTransactionComplete = function() {
    	NRS.goToPage("scheduled_transactions");
	};

    NRS.incoming.transactions = function() {
		NRS.loadPage("transactions");
	};

	NRS.setup.transactions = function() {
		var sidebarId = 'dashboard_link';
		var options = {
			"id": sidebarId,
			"titleHTML": '<i class="mdi mdi-av-timer fa-fw" data-icon="v"></i><span class="hide-menu"> Dashboard <span class="fa arrow"></span></span>',
			"page": 'dashboard',
			"desiredPosition": 10
		};
		NRS.addTreeviewSidebarMenuItem(options);
		options = {
			"titleHTML": '<span class="hide-menu" data-i18n="dashboard">Dashboard</span>',
			"type": 'PAGE',
			"page": 'dashboard'
		};
		NRS.appendMenuItemToTSMenuItem(sidebarId, options);
		options = {
			"titleHTML": '<span class="hide-menu" data-i18n="account_ledger">Account Ledger</span>',
			"type": 'PAGE',
			"page": 'ledger'
		};
		NRS.appendMenuItemToTSMenuItem(sidebarId, options);
		options = {
			"titleHTML": '<span class="hide-menu" data-i18n="account_properties">Account Properties</span>',
			"type": 'PAGE',
			"page": 'account_properties'
		};
		NRS.appendMenuItemToTSMenuItem(sidebarId, options);
		options = {
			"titleHTML": '<span class="hide-menu" data-i18n="my_transactions">My Transactions</span>',
			"type": 'PAGE',
			"page": 'transactions'
		};
		NRS.appendMenuItemToTSMenuItem(sidebarId, options);
		options = {
			"titleHTML": '<span class="hide-menu" data-i18n="approval_requests">Approval Requests</span>',
			"type": 'PAGE',
			"page": 'approval_requests_account'
		};
		NRS.appendMenuItemToTSMenuItem(sidebarId, options);
	};

	$(document).on("click", "#transactions_type_navi li a", function(e) {
		e.preventDefault();
		$('#transactions_type_navi').find('li.active').removeClass('active');
  		$(this).parent('li').addClass('active');
  		NRS.buildTransactionsSubTypeNavi();
  		NRS.pageNumber = 1;
		NRS.loadPage("transactions");
	});

	$(document).on("click", "#transactions_sub_type_navi li a", function(e) {
		e.preventDefault();
		$('#transactions_sub_type_navi').find('li.active').removeClass('active');
  		$(this).parent('li').addClass('active');
  		NRS.pageNumber = 1;
		NRS.loadPage("transactions");
	});

	$(document).on("click", "#transactions_sub_type_show_hide_btn", function(e) {
		e.preventDefault();
        var subTypeNaviBox = $('#transactions_sub_type_navi_box');
        if (subTypeNaviBox.is(':visible')) {
			subTypeNaviBox.hide();
			$(this).text($.t('show_type_menu', 'Show Type Menu'));
		} else {
			subTypeNaviBox.show();
			$(this).text($.t('hide_type_menu', 'Hide Type Menu'));
		}
	});

    var secretLedgerWor = $('#transaction_ledger_fill_secret_word_modal');
    $(document).on('submit', '#get_ledger_private_transactions', function(e){
        e.preventDefault();
        var formParams = $( this ).serializeArray();
        var API = '/apl?requestType=getPrivateBlockchainTransactions&secretPhrase=' + formParams[0].value;

        if (NRS.validatePassphrase(formParams[0].value, true)) {
            NRS.accountLedgerPagination.setPrivate(formParams[0].value);
        } else {
            $('#incorrect_passphrase_my_transactions').show();
        }
    });

    $('#open_send_money_private').click(function() {
        $('#send_money_modal').modal('hide');
        $('#send_money_private_modal').modal('show');
        $.growl($.t("success_send_money") + " <a href='#' data-account='" + NRS.getAccountFormatted(data, "recipient") + "' data-toggle='modal' data-target='#add_contact_modal' style='text-decoration:underline'>" + $.t("add_recipient_to_contacts_q") + "</a>", {
            "type": "success"
        });
    });
    
    $(document).on('click', '#open_scheduled_transactions', function() {
        $('#admin_password_modal').modal('show');
    });

    $('body').on('click', '#send_money_private', function() {
        var recipient  = $('#send_money_recipient_info').val();
        var amount     = $('#send_money_amount_info').val();
        var fee        = $('#send_money_fee_info').val();
        var passphrase = $('#send_money_password_info').val();

        var url = API;
        url += 'requestType=sendMoneyPrivate';
	    
        var data = {
            deadline:     1440,
            feeATM:       fee + '00000000',
            amountATM:    amount + '00000000',
            recipient :   recipient,
            secretPhrase: passphrase
        };

        if (NRS.validatePassphrase(passphrase)) {
            $('#incorrect_passphrase_message').removeClass('active');

            $.ajax({
                type: "POST",
                url: url,
                data: data,
                success: function(res) {
                    $('#send_money_modal').modal('hide');
                    $('#send_money_private_modal').modal('hide');
                }
            });
        } else {
            $('#incorrect_passphrase_message').addClass('active');
        }
    });

    var that = this;
    $(document).on('submit', '#get_private_transactions', function(e) {
        e.preventDefault();
        var formParams = $( this ).serializeArray();
        var API = '/apl?requestType=getPrivateBlockchainTransactions&secretPhrase=' + formParams[0].value;

        if (NRS.validatePassphrase(formParams[0].value, true)) {

            NRS.accountLedgerPagination.setKeys(formParams[0].value);
            NRS.accountLedgerPagination.setPrivate();


            $('#transaction_fill_secret_word_modal').modal('hide');
            $('#incorrect_passphrase_my_transactions').hide();

            NRS.myTransactionPagination.setKeys(formParams[0].value);
            NRS.myTransactionPagination.setPrivate();

            $('#transaction_fill_secret_word_modal').modal('hide');
            $('#incorrect_passphrase_my_transactions').hide();


		} else {
            $('#incorrect_passphrase_my_transactions').show();
        }
    });

    $(document).on('submit', '#get_ledger_private_transactions', function(e) {
        e.preventDefault();
        var formParams = $( this ).serializeArray();

        var API = '/apl?requestType=getPrivateAccountLedger&secretPhrase=' + formParams[0].value;

        if (NRS.validatePassphrase(formParams[0].value, true)) {

            NRS.accountLedgerPagination.setKeys(formParams[0].value);
            NRS.accountLedgerPagination.setPrivate();

            $('#transaction_ledger_fill_secret_word_modal').modal('hide');
            $('#incorrect_passphrase_my_ledger').hide();

        } else {
            $('#incorrect_passphrase_my_ledger').show();
        }

    });
    
    $(document).on('submit', '#get_scheduled_transactions', function(e) {
    	e.preventDefault();
    	
	    var adminPassword = $( this ).serializeArray();
	        adminPassword = adminPassword[0].value;
	
	
	    NRS.sendRequest("getScheduledTransactions+", {
		        // account: NRS.accountRS,
			    adminPassword: adminPassword
		    }, function(response) {
		        var errorMessage = $("#scheduled_transactions_error_message");
		        if (response.errorCode) {
		            errorMessage.text(NRS.unescapeRespStr(response.errorDescription));
		            errorMessage.show();
		            
			        return;
		        } else {
		            errorMessage.hide();
		            errorMessage.text("");
		            
		            return;
		    }
		    var rows = "";
		    
		    $('#scheduled_transactions_box').addClass('active');
		
		    if (response.scheduledTransactions && response.scheduledTransactions.length) {
	            if (response.scheduledTransactions.length > NRS.itemsPerPage) {
	                NRS.hasMorePages = true;
	                response.scheduledTransactions.pop();
	            }
	            var decimals = NRS.getTransactionsAmountDecimals(response.scheduledTransactions);
	            for (var i = 0; i < response.scheduledTransactions.length; i++) {
	                var transaction = response.scheduledTransactions[i];
		            rows += NRS.getTransactionRowHTML(transaction, ["delete"], decimals, true);
	            }
	        }
	        NRS.dataLoaded(rows);
	    });
	    
    });
    
    $(document).on('click',  '#open_send_modal_footer', function() {
        $("#send_modal_footer_alert").hide();
        $("#send_money_private_modal").find('.modal-footer').removeClass('disabled');
    });

	return NRS;
}(NRS || {}, jQuery));
