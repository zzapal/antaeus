/*
    Implements the data access layer (DAL).
    This file implements the database queries used to fetch and insert rows in our database tables.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class AntaeusDal(private val db: Database) {
    fun fetchInvoice(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            InvoiceTable
                .select { InvoiceTable.id.eq(id) }
                .firstOrNull()
                ?.toInvoice()
        }
    }

    fun fetchInvoices(): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                .selectAll()
                .map { it.toInvoice() }
        }
    }

    fun fetchPendingInvoices() : List<Invoice> {
        return transaction(db) {
            InvoiceTable
                    .select{ InvoiceTable.status.eq(InvoiceStatus.PENDING.toString()) }
                    .map{ it.toInvoice() }
        }
    }

    fun createInvoice(amount: Money, customer: Customer, status: InvoiceStatus = InvoiceStatus.PENDING): Invoice? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                .insert {
                    it[this.value] = amount.value
                    it[this.currency] = amount.currency.toString()
                    it[this.status] = status.toString()
                    it[this.customerId] = customer.id
                } get InvoiceTable.id
        }

        return fetchInvoice(id!!)
    }

    fun fetchCustomer(id: Int): Customer? {
        return transaction(db) {
            CustomerTable
                .select { CustomerTable.id.eq(id) }
                .firstOrNull()
                ?.toCustomer()
        }
    }

    fun fetchCustomers(): List<Customer> {
        return transaction(db) {
            CustomerTable
                .selectAll()
                .map { it.toCustomer() }
        }
    }

    fun createCustomer(currency: Currency): Customer? {
        val id = transaction(db) {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = currency.toString()
            } get CustomerTable.id
        }

        return fetchCustomer(id!!)
    }

    fun updateInvoiceStatus(invoice: Invoice, status: InvoiceStatus) {
        transaction(db) {
            addLogger(StdOutSqlLogger)
            InvoiceTable.update({InvoiceTable.id.eq(invoice.id)}) {
                it[this.status] = status.toString()
            }
        }
    }

    fun createPaymentAudit(arguments: String): Int {
        val id = transaction(db) {
            addLogger(StdOutSqlLogger)

            PaymentAuditTable.insert {
                it[this.arguments] = arguments
            } get PaymentAuditTable.id
        }

        return id!!
    }

    fun updatePaymentAudit(id: Int, tookMs: Long, response: String) {
        transaction(db) {
            addLogger(StdOutSqlLogger)

            PaymentAuditTable.update({PaymentAuditTable.id.eq(id)}) {
                it[this.tookMs] = tookMs
                it[this.response] = response
            }
        }
    }
}
