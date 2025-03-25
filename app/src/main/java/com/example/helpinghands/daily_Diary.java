package com.example.helpinghands;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class daily_Diary extends AppCompatActivity {

    private EditText studentName, siteOrganization, siteSupervisor, timeIn, timeOut, activitiesDone, hoursWorked;
    private TextView semesterText, logDate;
    private Button submitButton, downloadReportButton;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private String userId, currentDate, currentSemester;
    private static final int MAX_ENTRIES_PER_WEEK = 3;
    private static final int MAX_HOURS_PER_WEEK = 9;
    private static final String TAG = "DailyDiary";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_daily_diary);
        // Disable page number announcements by hiding the action bar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        // Ensure the title is not spoken by TalkBack
        setTitle(" ");


        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        userId = (user != null) ? user.getUid() : null;

        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        currentDate = getCurrentDate();
        currentSemester = getCurrentSemester();
        logDate.setText(currentDate);
        semesterText.setText(currentSemester);

        retrieveStudentDetails();  // Load stored student info
        fetchAndValidateSite();    // Always fetch site info
        calculateHours();          // Auto-compute worked hours

        submitButton.setOnClickListener(v -> checkAndSaveLog());
        downloadReportButton.setOnClickListener(v -> generatePdfReport());
    }

    private void initializeViews() {
        studentName = findViewById(R.id.studentName);
        semesterText = findViewById(R.id.semester);
        siteOrganization = findViewById(R.id.siteOrganization);
        siteSupervisor = findViewById(R.id.siteSupervisor);
        logDate = findViewById(R.id.logDate);
        timeIn = findViewById(R.id.timeIn);
        timeOut = findViewById(R.id.timeOut);
        activitiesDone = findViewById(R.id.activitiesDone);
        hoursWorked = findViewById(R.id.hoursWorked);
        submitButton = findViewById(R.id.submit);
        downloadReportButton = findViewById(R.id.report);
    }

    private String getCurrentSemester() {
        int month = Calendar.getInstance().get(Calendar.MONTH) + 1;
        if (month >= 1 && month <= 4) return "Spring (Jan-April)";
        if (month >= 5 && month <= 8) return "Summer (May-August)";
        return "Fall (Sept-December)";
    }

    private String getCurrentDate() {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime());
    }

    private void retrieveStudentDetails() {
        // Fetch the latest log entry for the user
        db.collection("daily_logs").document(userId).collection("logs")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING) // Sort by timestamp in descending order
                .limit(1) // Limit to the most recent log
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Get the most recent log
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        String studentNameFromLog = document.getString("studentName");
                        String siteSupervisorFromLog = document.getString("siteSupervisor");

                        // Update the UI with the retrieved data
                        if (studentNameFromLog != null) {
                            studentName.setText(studentNameFromLog);
                        }
                        if (siteSupervisorFromLog != null) {
                            siteSupervisor.setText(siteSupervisorFromLog);
                        }
                    } else {
                        Toast.makeText(this, "No logs found for the user", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load student details", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching student details", e);
                });
    }

    private void fetchAndValidateSite() {
        DocumentReference siteRef = db.collection("UserStates").document(userId);
        siteRef.get().addOnSuccessListener(document -> {
            if (document.exists() && document.contains("selectedSite")) {
                siteOrganization.setText(document.getString("selectedSite"));
            } else {
                siteOrganization.setText("");  // Clear the field
                clearAllUserLogs();  // Clear logs if site is missing
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Error fetching site details", Toast.LENGTH_SHORT).show()
        );
    }

    private void clearAllUserLogs() {
        db.collection("daily_logs").document(userId)
                .delete()
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "User logs deleted due to dropped site", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error deleting user logs", Toast.LENGTH_SHORT).show()
                );
    }

    private void calculateHours() {
        TextWatcher timeWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!timeIn.getText().toString().isEmpty() && !timeOut.getText().toString().isEmpty()) {
                    hoursWorked.setText(String.valueOf(computeHours(timeIn.getText().toString(), timeOut.getText().toString())));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        timeIn.addTextChangedListener(timeWatcher);
        timeOut.addTextChangedListener(timeWatcher);
    }

    private int computeHours(String inTime, String outTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            long startTime = sdf.parse(inTime).getTime();
            long endTime = sdf.parse(outTime).getTime();
            return (int) ((endTime - startTime) / (1000 * 60 * 60));
        } catch (Exception e) {
            return 0;
        }
    }

    private void checkAndSaveLog() {
        db.collection("daily_logs").document(userId).collection("logs")
                .whereEqualTo("date", currentDate)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Toast.makeText(this, "You have already logged today!", Toast.LENGTH_SHORT).show();
                    } else {
                        checkWeeklyEntries();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error checking log entry", Toast.LENGTH_SHORT).show()
                );
    }

    private void checkWeeklyEntries() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); // Start of the week (Monday)
        String weekStartDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.getTime());

        db.collection("daily_logs").document(userId).collection("logs")
                .whereGreaterThanOrEqualTo("date", weekStartDate) // Fetch logs for the current week
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.size() >= MAX_ENTRIES_PER_WEEK) {
                        // Check if the maximum number of entries (3) has been reached
                        Toast.makeText(this, "Maximum of 3 logs per week reached!", Toast.LENGTH_SHORT).show();
                    } else {
                        // Calculate total hours logged in the current week
                        int totalHoursThisWeek = 0;
                        for (DocumentSnapshot doc : querySnapshot) {
                            String hoursStr = doc.getString("hours");
                            if (hoursStr != null && !hoursStr.isEmpty()) {
                                totalHoursThisWeek += Integer.parseInt(hoursStr);
                            }
                        }

                        // Get the hours for the current log entry
                        int currentLogHours = Integer.parseInt(hoursWorked.getText().toString());

                        // Check if adding the current log exceeds the maximum allowed hours (9)
                        if (totalHoursThisWeek + currentLogHours > MAX_HOURS_PER_WEEK) {
                            Toast.makeText(this, "Maximum of 9 hours per week reached!", Toast.LENGTH_SHORT).show();
                        } else {
                            // Save the log if the limit is not exceeded
                            saveDailyLog();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking weekly logs", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching weekly logs", e);
                });
    }

    private void saveDailyLog() {
        // Validate hours worked
        int currentLogHours = Integer.parseInt(hoursWorked.getText().toString());
        if (currentLogHours <= 0) {
            Toast.makeText(this, "Invalid hours worked. Please enter a valid value.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Proceed to save the log
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("studentName", studentName.getText().toString());
        logEntry.put("semester", semesterText.getText().toString());
        logEntry.put("siteOrganization", siteOrganization.getText().toString());
        logEntry.put("siteSupervisor", siteSupervisor.getText().toString());
        logEntry.put("date", currentDate);
        logEntry.put("timeIn", timeIn.getText().toString());
        logEntry.put("timeOut", timeOut.getText().toString());
        logEntry.put("activities", activitiesDone.getText().toString());
        logEntry.put("hours", hoursWorked.getText().toString());
        logEntry.put("timestamp", Timestamp.now());

        db.collection("daily_logs").document(userId).collection("logs").add(logEntry)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Log saved successfully", Toast.LENGTH_SHORT).show();
                    calculateTotalHours(); // Update total hours in the database
                    calculateTotalWeeks(); // Update total weeks in the database
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving log", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error saving log", e);
                });
    }

    private void calculateTotalHours() {
        db.collection("daily_logs").document(userId).collection("logs")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalHours = 0;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String hoursStr = doc.getString("hours");
                        if (hoursStr != null && !hoursStr.isEmpty()) {
                            totalHours += Integer.parseInt(hoursStr);
                        }
                    }

                    // Update total hours in the database
                    db.collection("students").document(userId)
                            .update("totalHours", totalHours)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Total hours updated in database"))
                            .addOnFailureListener(e -> Log.e(TAG, "Error updating total hours", e));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error calculating total hours", Toast.LENGTH_SHORT).show()
                );
    }

    private void calculateTotalWeeks() {
        db.collection("daily_logs").document(userId).collection("logs")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<Integer> weeks = new HashSet<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String dateStr = doc.getString("date");
                        if (dateStr != null && !dateStr.isEmpty()) {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(sdf.parse(dateStr));
                                int week = calendar.get(Calendar.WEEK_OF_YEAR);
                                weeks.add(week);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing date: " + dateStr, e);
                            }
                        }
                    }

                    // Update total weeks in the database
                    db.collection("students").document(userId)
                            .update("totalWeeks", weeks.size())
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Total weeks updated in database"))
                            .addOnFailureListener(e -> Log.e(TAG, "Error updating total weeks", e));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error calculating total weeks", Toast.LENGTH_SHORT).show()
                );
    }

    private void moveFileToDownloads(File sourceFile, String fileName) {
        // Get the URI for the file using FileProvider
        Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", sourceFile);

        // Create an Intent to share the file
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(contentUri, "application/pdf");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Verify that the intent can be resolved
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "No app available to open the PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void generatePdfReport() {
        // Show a progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Generating report...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Fetch logs from Firestore
        db.collection("daily_logs").document(userId).collection("logs")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Generate a unique file name using a timestamp
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                    String fileName = "report_" + timeStamp + ".pdf"; // Unique file name
                    File tempFile = new File(getExternalFilesDir(null), fileName);

                    try {
                        PdfWriter writer = new PdfWriter(tempFile);
                        PdfDocument pdf = new PdfDocument(writer);
                        Document document = new Document(pdf);

                        // Add student details
                        document.add(new Paragraph("Student Name: " + studentName.getText().toString())
                                .setTextAlignment(TextAlignment.LEFT)
                                .setMarginBottom(10));
                        document.add(new Paragraph("Semester: " + semesterText.getText().toString())
                                .setMarginBottom(10));
                        document.add(new Paragraph("Site Organization: " + siteOrganization.getText().toString())
                                .setMarginBottom(10));
                        document.add(new Paragraph("Site Supervisor: " + siteSupervisor.getText().toString())
                                .setMarginBottom(20));

                        // Add logs table
                        Table table = new Table(5); // 5 columns: Date, Time In, Time Out, Hours, Activities
                        table.setWidth(UnitValue.createPercentValue(100));
                        table.addHeaderCell("Date");
                        table.addHeaderCell("Time In");
                        table.addHeaderCell("Time Out");
                        table.addHeaderCell("Hours");
                        table.addHeaderCell("Activities");

                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            table.addCell(doc.getString("date"));
                            table.addCell(doc.getString("timeIn"));
                            table.addCell(doc.getString("timeOut"));
                            table.addCell(doc.getString("hours"));
                            table.addCell(doc.getString("activities"));
                        }
                        document.add(table);

                        // Add spacing after the table
                        document.add(new Paragraph("\n"));

                        // Add signature section
                        document.add(new Paragraph("Signatures:").setBold().setMarginBottom(10));

                        // Create a table for signatures (2 columns: Student and Supervisor)
                        Table signatureTable = new Table(2); // 2 columns
                        signatureTable.setWidth(UnitValue.createPercentValue(100)); // Full width

                        // Student Signature
                        signatureTable.addCell(new Cell().add(new Paragraph("Student Signature:"))
                                .setBorder(Border.NO_BORDER));
                        signatureTable.addCell(new Cell().add(new Paragraph("Supervisor Signature:"))
                                .setBorder(Border.NO_BORDER));

                        // Add signature lines
                        signatureTable.addCell(new Cell().add(new Paragraph("-------------------"))
                                .setBorder(Border.NO_BORDER));
                        signatureTable.addCell(new Cell().add(new Paragraph("-------------------"))
                                .setBorder(Border.NO_BORDER));

                        // Add spacing between signature lines and site stamp
                        signatureTable.addCell(new Cell().add(new Paragraph(" "))
                                .setBorder(Border.NO_BORDER));
                        signatureTable.addCell(new Cell().add(new Paragraph(" "))
                                .setBorder(Border.NO_BORDER));

                        document.add(signatureTable);

                        // Add Site Stamp
                        document.add(new Paragraph("Site Stamp:")
                                .setMarginBottom(5));
                        document.add(new Paragraph("[Place Stamp Here]")
                                .setMarginBottom(20));

                        document.close();

                        // Save the PDF to Downloads
                        savePdfToDownloads(tempFile, fileName);

                        // Hide progress dialog
                        progressDialog.dismiss();
                        Toast.makeText(this, "Report generated and saved to Downloads", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Failed to generate report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error generating PDF", e);
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to fetch logs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching logs", e);
                });
    }

    private void savePdfToDownloads(File file, String fileName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10 and above, use MediaStore
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            ContentResolver resolver = getContentResolver();
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

            if (uri != null) {
                try (OutputStream outputStream = resolver.openOutputStream(uri)) {
                    if (outputStream != null) {
                        try (FileInputStream inputStream = new FileInputStream(file)) {
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = inputStream.read(buffer)) > 0) {
                                outputStream.write(buffer, 0, length);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to save PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            // For Android 9 and below, use the old method
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File destinationFile = new File(downloadsDir, fileName);

            try {
                copyFile(file, destinationFile);
                Toast.makeText(this, "Report saved to Downloads", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to save PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void copyFile(File sourceFile, File destFile) throws IOException {
        try (InputStream in = new FileInputStream(sourceFile);
             OutputStream out = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }
}