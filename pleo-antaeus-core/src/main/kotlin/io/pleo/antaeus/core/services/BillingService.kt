package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
    fun chargeAllInvoices() {
        for (invoice in invoiceService.fetchPendingInvoices()) {
            paymentProvider.charge(invoice)
            invoiceService.invoicePaid(invoice)
        }
    }
}