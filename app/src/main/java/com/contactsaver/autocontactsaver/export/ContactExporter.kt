package com.contactsaver.autocontactsaver.export

import android.content.Context
import android.os.Environment
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility for exporting contacts to different formats
 */
object ContactExporter {

    data class ContactData(
        val name: String,
        val phoneNumber: String,
        val email: String = "",
        val notes: String = ""
    )

    enum class ExportFormat {
        EXCEL, CSV, VCF
    }

    /**
     * Export contacts to selected format
     */
    fun exportContacts(
        context: Context,
        contacts: List<ContactData>,
        format: ExportFormat
    ): Result<File> {
        return try {
            val file = when (format) {
                ExportFormat.EXCEL -> exportToExcel(context, contacts)
                ExportFormat.CSV -> exportToCSV(context, contacts)
                ExportFormat.VCF -> exportToVCF(context, contacts)
            }
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Export to Excel format (.xlsx)
     */
    private fun exportToExcel(context: Context, contacts: List<ContactData>): File {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Contacts")

        // Create header row
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("Name")
        headerRow.createCell(1).setCellValue("Phone Number")
        headerRow.createCell(2).setCellValue("Email")
        headerRow.createCell(3).setCellValue("Notes")

        // Add data rows
        contacts.forEachIndexed { index, contact ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(contact.name)
            row.createCell(1).setCellValue(contact.phoneNumber)
            row.createCell(2).setCellValue(contact.email)
            row.createCell(3).setCellValue(contact.notes)
        }

        // Auto-size columns
        for (i in 0..3) {
            sheet.autoSizeColumn(i)
        }

        // Save to file
        val file = createExportFile(context, "xlsx")
        FileOutputStream(file).use { outputStream ->
            workbook.write(outputStream)
        }
        workbook.close()

        return file
    }

    /**
     * Export to CSV format
     */
    private fun exportToCSV(context: Context, contacts: List<ContactData>): File {
        val file = createExportFile(context, "csv")
        
        FileOutputStream(file).bufferedWriter().use { writer ->
            // Write header
            writer.write("Name,Phone Number,Email,Notes\n")
            
            // Write data
            contacts.forEach { contact ->
                val line = listOf(
                    escapeCSV(contact.name),
                    escapeCSV(contact.phoneNumber),
                    escapeCSV(contact.email),
                    escapeCSV(contact.notes)
                ).joinToString(",")
                writer.write("$line\n")
            }
        }

        return file
    }

    /**
     * Export to VCF format (vCard)
     */
    private fun exportToVCF(context: Context, contacts: List<ContactData>): File {
        val file = createExportFile(context, "vcf")
        
        FileOutputStream(file).bufferedWriter().use { writer ->
            contacts.forEach { contact ->
                writer.write("BEGIN:VCARD\n")
                writer.write("VERSION:3.0\n")
                writer.write("FN:${contact.name}\n")
                writer.write("TEL;TYPE=CELL:${contact.phoneNumber}\n")
                
                if (contact.email.isNotEmpty()) {
                    writer.write("EMAIL:${contact.email}\n")
                }
                
                if (contact.notes.isNotEmpty()) {
                    writer.write("NOTE:${contact.notes}\n")
                }
                
                writer.write("END:VCARD\n")
            }
        }

        return file
    }

    /**
     * Create export file with timestamp
     */
    private fun createExportFile(context: Context, extension: String): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())
        val fileName = "contacts_$timestamp.$extension"
        
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloadsDir, fileName)
    }

    /**
     * Escape CSV special characters
     */
    private fun escapeCSV(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    /**
     * Get file extension name for format
     */
    fun getFormatExtension(format: ExportFormat): String {
        return when (format) {
            ExportFormat.EXCEL -> "xlsx"
            ExportFormat.CSV -> "csv"
            ExportFormat.VCF -> "vcf"
        }
    }

    /**
     * Get format display name
     */
    fun getFormatName(format: ExportFormat): String {
        return when (format) {
            ExportFormat.EXCEL -> "Excel (.xlsx)"
            ExportFormat.CSV -> "CSV (.csv)"
            ExportFormat.VCF -> "vCard (.vcf)"
        }
    }
}
