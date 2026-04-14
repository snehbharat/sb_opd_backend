package com.sbpl.OPD.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ReceiptFormatter {
    
    public static String formatReceipt(Map<String, Object> receiptData) {
        StringBuilder receipt = new StringBuilder();
        
        // Header
        receipt.append("=================================== RECEIPT ===================================\n");
        
        // Patient Information
        receipt.append("Patient Name : ").append(receiptData.get("patientName")).append("\n");
        receipt.append("Age : ").append(receiptData.get("patientAge")).append("\n");
        receipt.append("State Code : ").append(receiptData.get("stateCode")).append("\n");
        receipt.append("Place of Supply :").append(receiptData.get("placeOfSupply")).append("\n");
        receipt.append("Address : ").append(receiptData.get("patientAddress")).append("\n");
        receipt.append("Contact/Email : ").append(receiptData.get("patientContact")).append("\n");
        receipt.append("\n");
        
        // Doctor Information
        receipt.append("Doctor Name : ").append(receiptData.get("doctorName")).append("\n");
        receipt.append("Degree/Regd No : ").append(receiptData.get("doctorSpecialization")).append(", \n");
        receipt.append(receiptData.get("doctorRegistrationNo")).append("\n");
        receipt.append("GSTIN (HCP if any) :\n");
        receipt.append("State Code : ").append(receiptData.get("stateCode")).append("\n");
        receipt.append("Contact/Email : ").append(receiptData.get("doctorContact")).append("\n");
        receipt.append("\n");
        
        // Itemized list header
        receipt.append("Sr No.  Description of Service          HSN      Fee    IGST   CGST   SGST   Discount  Refund   Amount\n");
        receipt.append("------  -----------------------------  ------  -------  ----  -----  -----  --------  ------  -------\n");
        
        // Items
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) receiptData.get("items");
        if (items == null || items.isEmpty()) {
            receipt.append("No items found in receipt\n");
            return receipt.toString();
        }
        
        BigDecimal totalFee = BigDecimal.ZERO;
        BigDecimal totalIGST = BigDecimal.ZERO;
        BigDecimal totalCGST = BigDecimal.ZERO;
        BigDecimal totalSGST = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (Map<String, Object> item : items) {
            String serialNumber = item.get("serialNumber") != null ? item.get("serialNumber").toString() : "N/A";
            String description = item.get("description") != null ? item.get("description").toString() : "N/A";
            String hsnCode = item.get("hsnCode") != null ? item.get("hsnCode").toString() : "N/A";
            BigDecimal fee = (BigDecimal) (item.get("fee") != null ? item.get("fee") : BigDecimal.ZERO);
            BigDecimal igst = (BigDecimal) (item.get("igst") != null ? item.get("igst") : BigDecimal.ZERO);
            BigDecimal cgst = (BigDecimal) (item.get("cgst") != null ? item.get("cgst") : BigDecimal.ZERO);
            BigDecimal sgst = (BigDecimal) (item.get("sgst") != null ? item.get("sgst") : BigDecimal.ZERO);
            BigDecimal discount = (BigDecimal) (item.get("discount") != null ? item.get("discount") : BigDecimal.ZERO);
            BigDecimal refund = (BigDecimal) (item.get("refund") != null ? item.get("refund") : BigDecimal.ZERO);
            BigDecimal amount = (BigDecimal) (item.get("amount") != null ? item.get("amount") : BigDecimal.ZERO);
            
            receipt.append(String.format("%-6s  %-29s  %-6s  %7s  %4s  %5s  %5s  %8s  %6s  %7s%n", 
                serialNumber, 
                description.substring(0, Math.min(description.length(), 29)), 
                hsnCode,
                formatCurrency(fee),
                formatCurrency(igst),
                formatCurrency(cgst),
                formatCurrency(sgst),
                formatCurrency(discount),
                formatCurrency(refund),
                formatCurrency(amount)));
                
            totalFee = totalFee.add(fee);
            totalIGST = totalIGST.add(igst);
            totalCGST = totalCGST.add(cgst);
            totalSGST = totalSGST.add(sgst);
            totalDiscount = totalDiscount.add(discount);
            totalRefund = totalRefund.add(refund);
            totalAmount = totalAmount.add(amount);
        }
        
        receipt.append("------  -----------------------------  ------  -------  ----  -----  -----  --------  ------  -------\n");
        
        // Totals
        receipt.append(String.format("Total %64s  %4s  %5s  %5s  %8s  %6s  %7s%n",
            formatCurrency(totalFee != null ? totalFee : BigDecimal.ZERO),
            formatCurrency(totalIGST != null ? totalIGST : BigDecimal.ZERO),
            formatCurrency(totalCGST != null ? totalCGST : BigDecimal.ZERO),
            formatCurrency(totalSGST != null ? totalSGST : BigDecimal.ZERO),
            formatCurrency(totalDiscount != null ? totalDiscount : BigDecimal.ZERO),
            formatCurrency(totalRefund != null ? totalRefund : BigDecimal.ZERO),
            formatCurrency(totalAmount != null ? totalAmount : BigDecimal.ZERO)));
        
        // Payment information
        receipt.append("\n");
        receipt.append(formatPaymentDate(receiptData.get("paymentDate"))).append(" Paid At Clinic By Credit/Debit Card\n");
        BigDecimal finalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        receipt.append(formatCurrency(finalAmount)).append(" Total Amount ").append(formatCurrency(finalAmount)).append("\n");
        receipt.append("\n");
        receipt.append("Amount in words: ").append(numberToWords(finalAmount.intValue())).append("\n");
        receipt.append("Net Amount Received ").append(formatCurrency(finalAmount)).append("\n");
        
        receipt.append("=============================================================================\n");
        
        return receipt.toString();
    }
    
    private static String formatPaymentDate(Object dateObj) {
        if (dateObj == null) {
            return "N/A";
        }
        
        try {
            if (dateObj instanceof String) {
                String dateStr = (String) dateObj;
                // Parse string date to Date object and format it nicely
                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd[ HH:mm:ss.SSS]['T'HH:mm:ss.SSS]");
                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
                
                // Handle different date formats
                LocalDateTime dateTime;
                if (dateStr.contains(" ")) {
                    dateTime = LocalDateTime.parse(dateStr, inputFormatter);
                } else if (dateStr.contains("T")) {
                    dateTime = LocalDateTime.parse(dateStr, inputFormatter);
                } else {
                    LocalDate date = LocalDate.parse(dateStr);
                    dateTime = date.atStartOfDay();
                }
                
                return dateTime.format(outputFormatter);
            }
        } catch (Exception e) {
            // If parsing fails, try another approach
            try {
                String str = dateObj.toString();
                if (str.contains("T")) {
                    LocalDateTime dt = LocalDateTime.parse(str);
                    return dt.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
                }
            } catch (Exception ex) {
                // Return original string if all parsing attempts fail
            }
        }
        
        return dateObj.toString();
    }
    
    private static String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "₹0.00";
        }
        
        DecimalFormat formatter = new DecimalFormat("#,##0.00");
        String formatted = formatter.format(amount.abs()); // Use abs to handle negative discounts
        
        // Add negative sign for negative values
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return "-₹" + formatted;
        }
        
        return "₹" + formatted;
    }
    
    // Enhanced number to words conversion for Indian currency
    public static String numberToWords(int number) {
        if (number == 0) {
            return "Zero Rupees Only";
        }
        
        String[] ones = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"
        };
        
        String[] tens = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
        };
        
        StringBuilder result = new StringBuilder();
        
        // Handle crores (1,00,00,000)
        if (number >= 10000000) {
            int crore = number / 10000000;
            result.append(ones[crore]).append(" Crore ");
            number %= 10000000;
        }
        
        // Handle lakhs (1,00,000)
        if (number >= 100000) {
            int lakh = number / 100000;
            result.append(ones[lakh]).append(" Lakh ");
            number %= 100000;
        }
        
        // Handle thousands (1,000)
        if (number >= 1000) {
            int thousand = number / 1000;
            if (thousand >= 100) {
                int hundred = thousand / 100;
                result.append(ones[hundred]).append(" Hundred ");
                thousand %= 100;
                if (thousand > 0) {
                    if (thousand < 20) {
                        result.append(ones[thousand]).append(" ");
                    } else {
                        result.append(tens[thousand / 10]).append(" ");
                        if (thousand % 10 > 0) {
                            result.append(ones[thousand % 10]).append(" ");
                        }
                    }
                }
                result.append("Thousand ");
            } else if (thousand < 20) {
                result.append(ones[thousand]).append(" Thousand ");
            } else {
                result.append(tens[thousand / 10]).append(" ");
                if (thousand % 10 > 0) {
                    result.append(ones[thousand % 10]).append(" ");
                }
                result.append("Thousand ");
            }
            number %= 1000;
        }
        
        // Handle hundreds
        if (number >= 100) {
            int hundred = number / 100;
            result.append(ones[hundred]).append(" Hundred ");
            number %= 100;
        }
        
        // Handle tens and ones
        if (number > 0) {
            if (number < 20) {
                result.append(ones[number]).append(" ");
            } else {
                result.append(tens[number / 10]);
                if (number % 10 > 0) {
                    result.append(" ").append(ones[number % 10]);
                }
                result.append(" ");
            }
        }
        
        result.append("Rupees Only");
        
        return result.toString().trim();
    }
}