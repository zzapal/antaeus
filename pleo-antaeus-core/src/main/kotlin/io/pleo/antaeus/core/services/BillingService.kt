package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import mu.KotlinLogging
import kotlinx.coroutines.*

private val logger = KotlinLogging.logger {}

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
    fun chargeAllInvoices() {
        runBlocking {
            for (invoice in invoiceService.fetchPendingInvoices()) {
                try {
                    logger.trace { "Charging invoice ${invoice.id}" }

                    if (!paymentProvider.charge(invoice)) {
                        logger.warn { "Invoice ${invoice.id} NOT charged - call returned FALSE" }
                    } else {
                        logger.trace { "Invoice ${invoice.id} charged sucessfully" }

                        invoiceService.invoicePaid(invoice)
                        logger.trace { "Updating invoice ${invoice.id} status - PAID" }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Invoice ${invoice.id} charging/updating invoice failed." }
                }
            }
        }
    }

    fun chargeAllInvoicesWithConcurrent() {
        runBlocking {
            for (invoice in invoiceService.fetchPendingInvoices()) {
                launch(Dispatchers.IO) {
                    try {
                        logger.trace { "Charging invoice ${invoice.id}" }

                        if (!paymentProvider.charge(invoice)) {
                            logger.warn { "Invoice ${invoice.id} NOT charged - call returned FALSE" }
                        } else {
                            logger.trace { "Invoice ${invoice.id} charged sucessfully" }

                            launch(Dispatchers.Main) {
                                invoiceService.invoicePaid(invoice)
                                logger.trace { "Updating invoice ${invoice.id} status - PAID" }
                            }
                        }
                    } catch (e: Exception) {
                        logger.error(e) { "Invoice ${invoice.id} charging/updating invoice failed." }
                    }
                }
            }
        }
    }
}