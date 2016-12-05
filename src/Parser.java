/*
 *Authors: Jacob Taylor
 */


import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.*;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.regex.Pattern;
import javax.swing.*;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;


interface TextAreaUpdater {
    void updateLog(String string);
}

public class Parser
{
    private int BEGIN_DATA = 5;
    private SQLConnect connect;
    //this holds the information about what quality level the value belongs to
    private ArrayList<String> qualityHeaders;
    private BufferedWriter writer;
    private JTextArea outputText;
    private TextAreaUpdater listener;
    StringBuffer buffer;
    Boolean parsing = false;

    public void setListener(TextAreaUpdater listener) {
        this.listener = listener;
    }

    public Parser(JTextArea text) throws IOException {
        connect = new SQLConnect();
        qualityHeaders = new ArrayList<>();
        buffer = new StringBuffer();
        outputText = text;
    }

    public boolean Parse(String path, StringBuilder usr, StringBuilder pswd) throws IOException
    {
        //outputText = text;
        parsing = true;
        //SendToGUI();

        if(!connect.connect(usr, pswd)) {
            usr.setLength(0);
            pswd.setLength(0);
            return false;
        }
        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(System.getProperty("user.dir") + "/error.sql")));
        usr.setLength(0);
        pswd.setLength(0);
        buffer.append("Reading!\n");
        listener.updateLog("Reading!\n");
        File f = new File (path);

        //if it is a directory traverse it else just read in the workbook directly
        if (f.isDirectory())
            TraverseDirectory (f);
        else
        {
            if(f.getName().endsWith(".xls")) {
                buffer.append("Parsing file ").append(f.getName()).append("\n");
                ReadInWorkbook(f);
            }
        }
        connect.disconnect();
        writer.close();
        buffer.append("Finished!\n");
        parsing = false;
        return true;
    }

    private void TraverseDirectory(File file)
    {
        //all directories that need to be visited
        Stack<File> directoryStack = new Stack<>();
        directoryStack.push(file);

        buffer.append("Starting parsing directory!\n");

        //this will go through all possible paths within the directory
        while (!directoryStack.isEmpty()) {
            //go through each directory and push all directories on the stack or work on the workbooks
            for (File a : directoryStack.pop().listFiles()) {
                //if it is not a directory then work on it and get ssoids
                if (!a.isDirectory()) {
                    buffer.append("Not dir\n");
                    if(a.getName().endsWith(".xls")) {
                        buffer.append("Parsing ").append(a.getName()).append("\n");
                        listener.updateLog("Parsing " + a.getName() + "\n");
                        ReadInWorkbook(a);
                    }

                }
                //push it to the stack
                else
                    directoryStack.push(a);

            }
        }

    }

    //appends the string to the output and adds a newline
    private void SendToGUI ()
    {
        Thread thread = new Thread(() -> {
            while(parsing) {
                System.out.println("Updating JTextPane");
                outputText.setText(outputText.getText() + buffer.toString());
                outputText.update(outputText.getGraphics());
                outputText.setCaretPosition(outputText.getDocument().getLength());
                buffer.setLength(0);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }


    private void ReadInWorkbook (File file)
    {
         /*
         * Each workbook is split into two parts! The header which contains information about the assessment
         * and the second part is the data area where all the information is.
         */
        Workbook workbook = null;
        FileInputStream inputStream = null;

        //flag to check if the program is in the data area:w
        boolean hasEnteredData = false;
        //the type of assesment like summative or formative
        String amountID = "";

        //the  id of the current course being parsed
        String courseID = "";


        try
        {
            inputStream = new FileInputStream(file);
            workbook = new HSSFWorkbook(inputStream);
        }
        catch (IOException e)
        {
            buffer.append("File not found\n");
        }

        Sheet firstSheet = null;
        try
        {
            firstSheet = workbook.getSheetAt(0);
        }catch (NullPointerException e)
        {
            System.out.println (e.toString());
        }

        Iterator<Row> iterator = null;
        if (firstSheet != null) {
            iterator = firstSheet.iterator();
        }


        //work on the the cells within the document
        while (iterator.hasNext())
        {
            Row nextRow = iterator.next();

            //look for the start of the data area
            if (!hasEnteredData)
            {
                Cell cell = null;
                if(nextRow.cellIterator().hasNext()) {
                    cell = nextRow.cellIterator().next();
                }


                //Student in first cell means that the next row will be the data area
                if (cell != null)
                {

                    if (cell.toString().toLowerCase().contains("evaluation"))
                    {
                        //Get the next cell over for the data to parse
                        cell = nextRow.getCell(1);
                        //If the next cell is empty or null, get the cell after that
                        if(cell.toString().equals("") || cell.toString() == null) {
                            cell = nextRow.getCell(2);
                        }
                        buffer.append("Evaluating: ").append(cell.toString()).append("\n");
                        listener.updateLog("Evaluating: " + cell.toString() + "\n");
                        //try to get the amount id from the 2nd cell in teh evaluation row
                        try
                        {
                            amountID = MapHeaderToAmountID(cell);
                        }catch (NoSuchElementException e)
                        {
                            buffer.append("Amount ID not able to be parsed!\n");
                        }

                        //try to get course id from 2nd cell in evaluation row
                        courseID = MapHeaderToCourseID(cell);
                        buffer.append("CourseID is ").append(courseID).append("\n");
                    }
                    else if (cell.toString().toLowerCase().compareTo("student") == 0)
                    {

                        //make sure no old headers from other files are still around
                        qualityHeaders.clear();

                        //the qualityHeaders start at BEGIN_DATA
                        int i = BEGIN_DATA;

                        cell = nextRow.getCell(i);

                        //get and store the string headers for each of the quality levels
                        while (cell != null && !cell.toString().isEmpty()) {

                            //map the header to the measurement id as well
                            qualityHeaders.add(MapHeaderToMeasurementID(cell));
                            System.out.println(i + ": " + cell.toString() + "\n");
                            i++;
                            cell = nextRow.getCell(i);
                        }
                        hasEnteredData = true;

                    }
                }
            }
            else
            {
                if(nextRow.cellIterator().hasNext())
                {
                    RowToDatabase(nextRow.cellIterator(), amountID, courseID);
                }

            }
        }

    }

    //This is called for all the rows that are not headers
    private void RowToDatabase (Iterator<Cell> iterator, String amountID, String courseID)
    {
        //Get the following variables from the row
        String student = iterator.next().toString();
        String stuID = RemoveScientificNotation(getStudentId(iterator.next().toString()));
        String evaluator = iterator.next().toString();
        String completionDate = FormatDate(iterator.next().toString());
        String isPublished = iterator.next().toString();

        Cell cell;


        String measurement;
        //pair quality header with actual amounts
        //For each header in the array, will be checking to see if they match what we want
        //then if they do, we will get the date from that cell beneath it
        for (String header : qualityHeaders)
        {
            cell = iterator.next();
            measurement = cell.toString();

            if(header.contains("MECE-QI") || header.contains("MOTS-QIW1")) {
                if(header.contains("MECE-QI")) {
                    measurement = DataToAnchorID(measurement);
                }
                else {
                    measurement = writingQualityToId(measurement);
                }
                if(isValidStuId(stuID)) {
                    try {
                        connect.insertObservationWith(GenerateAssessmentTrialID(stuID, amountID, courseID, convertCompletionDateToTerm(completionDate)), evaluator, "RATER", measurement, header, null, completionDate);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    //If not a valid student ID, replace it with the student name and write
                    //the error sql query to file so it can be fixed and then ran later to be
                    //inserted into the P2_EQS_OBS table
                    buffer.append(stuID).append(" not a valid stuID. Generating ErrorSQLQuery\n");
                    try {
                        writer.write(GenerateErrorSQLQuery(GenerateAssessmentTrialID(student, amountID, courseID, convertCompletionDateToTerm(completionDate)), evaluator, "RATER", measurement, header, null, completionDate));
                        writer.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            //No data whose header doesn't match these formats will be accepted
        }
    }

    //After everything, make sure we have a valid stuID. 8 digits exactly.
    private boolean isValidStuId(String stuID) {
        return Pattern.matches("[0-9]+", stuID) && stuID.length() == 8;
    }

    //Checks if External ID is a number or not. If not,
    //looks up the string in the table SSOID_MAPPINGS
    //and returns the correct EMPLID. Returns 000000000 otherwise
    private String getStudentId(String stuID) {
        boolean isNumber = true;
        String lookupResult = "";
        //Is the string passed in parsable as a number?
        try {
            //noinspection ResultOfMethodCallIgnored
            Float.parseFloat(stuID);
        } catch (NumberFormatException e) {
            //Nope. Try to look up what it is
            isNumber = false;
        }
        //Yup. Return it
        if(isNumber) {
            return stuID;
        }
        else {
            try {
                //Call the function in SQLConnect to look up the non-number string
                lookupResult = connect.getEmplidMappingFrom(stuID);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        //if we got 00000000 back from the DB, that means
        //there was not match to the non-digit stuID that was looked up
        //therefore, return the original string so that can be used as a possible
        //error string for the error.sql file
        if(lookupResult.equals("")) {
            return stuID;
        }
        //Got a good result from DB lookup. Return it
        else {
            return lookupResult;
        }

    }
    //takes in a cell and transforms the data into an Anchor ID
    //throws NoSuchElementException if no element is found
    //This could possibly be migrated to a DB lookup
    private String DataToAnchorID(String data)
    {
        data = data.toLowerCase();

        if (data.contains("emerging")) {
            return "MOTS-CLEV-EME2";
        } else if (data.contains("n/a")) {
            return "MOTS-CLEV-NA";
        } else if (data.contains("candidate")) {
            return "MOTS-CLEV-CAND";
        } else if (data.contains("below")) {
            return "MOTS-CLEV-BELO";
        } else if (data.contains("developing")) {
            return "MOTS-CLEV-DEVE";
        } else if (data.contains("revision")) {
            return "MOTS-CLEV-NA";
        } else if (data.contains("new"))  {
            return "MOTS-CLEV-EME1";
        } else {
            //return N/A for any other value
            //this may not be the correct approach
            return "MOTS-CLEV-NA";
        }
    }

    //Calls the SQLConnect function to look up matching
    //ANCHOR_ID to the measurement
    private String writingQualityToId(String data) {
        data = data.toLowerCase();
        String anchorID = "0000";
        try {
            anchorID = connect.getWritingQualityFrom(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return anchorID;
    }

    //Creates measurement ID from the quality indicator header
    private String MapHeaderToMeasurementID(Cell cell)
    {
        String cell_data = cell.toString().toLowerCase();
        StringBuilder builder = new StringBuilder();

        //If the head specifies 'Quality Indicator' and NOT 'Points', proceed
        if (cell_data.contains("quality indicator") && !cell_data.contains("points"))
        {
            //go through character by character to find the numbers for quality indicator
            for (char c : cell_data.toCharArray())
            {
                //if a character is a digit add it to digits for QI
                if (c >= '0' && c <= '9')
                {
                    builder.append(c);
                }
            }

            return "MECE-QI" + builder;
        }
        //If it is a writing quality header, just return the measurement ID
        else if (cell_data.contains("writing quality")) {
            return "MOTS-QIW1";
        }
        //If none of the above, leave it as is (well, lower cased though)
        //This way when the header is looped through later, only the data with correct
        //header types will be accessed
        else {
            return cell.toString();
        }
    }

    //maps the header for the completion of a evaluation to a amount id
    //p1 being formative and p2 being summative
    private String MapHeaderToAmountID(Cell cell) throws NoSuchElementException
    {
        String string = cell.toString().toLowerCase();

        if (string.contains("p2") || string.contains("practicum 2"))
        {
            string = "MEES-CESU-V001";
            buffer.append("Format is ").append(string).append("\n");
            return string;
        }
        else if (string.contains("p1"))
        {
            string = "MEES-CEFO-V001";
            buffer.append("Format is ").append(string).append("\n");
            return string;
        }
        else throw new NoSuchElementException();
    }

    //gets information about string values and uses them to find the course
    //that are stored in the database. This can be modified to include other
    //classes
    private String MapHeaderToCourseID (Cell cell)
    {

        String string = cell.toString().toLowerCase();
        String desc = "Practicum 2";

        if (string.contains("health"))
        {
            desc = "Health and PE";
        }
        else if (string.contains ("music"))
        {
            desc = "Music Education";
        }
        else if (string.contains("art")) {
            desc = "Art Education";
        }
        else if (string.contains("studio")) {
            desc = "Studio Schools";
        }
        else if (string.contains("certification")) {
            desc = "Teacher Certification";
        }
        else {
            buffer.append("Using default Practicum 2 CourseID\n");
        }
        try {
            desc = connect.getCourseIdFrom(desc);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return desc;

        //else throw new NoSuchElementException();
    }

    //Generates trial ID from candidate id (i.e. emplid), what type of assessment (i.e. summative, formative), course ID, classSection
    //(which is always 000), and term
    private String GenerateAssessmentTrialID(String candidateID, String amountID, String courseID, String term)
    {
        return candidateID + "-" + amountID + "-" + courseID + "-" + "000" + "-" + term;
    }

    //Generates and calls insert function
    private String GenerateErrorSQLQuery(String trialID, String empID, String actorType, String anchorID, String measurementID, String response, String observationDate)
    {

        return "INSERT INTO millerkei.P2_EQS_OBS (TRIAL_ID, EMPLID, ACTOR_TYPE, ANCHOR_ID, MMNT_ID, TEXT_RESPONSE, OBS_DT)" +
                " VALUES ('"+ trialID + "', '"+ empID +"', '"+actorType+"', '" + anchorID +"', '"+ measurementID +"', '" + response +"', '" + observationDate +"');";
    }

    //apache poi returns big numbers in scientific notation. This gets rid of it
    //Also prepends a 0 if the length is 7
    private String RemoveScientificNotation (String string)
    {
        try
        {
            DecimalFormat formatter = new DecimalFormat("###########");
            string = "" + formatter.format(Double.parseDouble(string));
        }
        catch (Exception e)
        {
            System.out.println (e.toString());
            return string;
        }

        if(string.length() == 7) {
            string = "0" + string;
        }
        return string;
    }

    //this puts the date in the correct format recognized by the database
    private String FormatDate (String date)
    {
        String month = MonthToDigits (date.substring(3,6));
        String day = date.substring(0, 2);
        String year = date.substring(7, 11);
        return year + "-" + month + "-" + day;
    }

    //returns a month as digits instead of abbreviations
    //because when parsing the cell with a date, it returns the
    //format of 12-Apr-2016 etc
    private String MonthToDigits (String month)
    {
        month = month.toLowerCase();

        if (month.contains("jan"))
            return "01";
        else if (month.contains("feb"))
            return "02";
        else if (month.contains("mar"))
            return "03";
        else if (month.contains("apr"))
            return "04";
        else if (month.contains("may"))
            return "05";
        else if (month.contains("jun"))
            return "06";
        else if (month.contains("jul"))
            return "07";
        else if (month.contains("aug"))
            return "08";
        else if (month.contains("sep"))
            return "09";
        else if (month.contains("oct"))
            return "10";
        else if (month.contains("nov"))
            return "11";
        else if (month.contains("dec"))
            return "12";

        return month;
    }

    //Takes in the date, calls the SQLConnect function to get the matching term
    //Returns 0000 if an error
    private String convertCompletionDateToTerm(String date)
    {
        String term = "0000";
        try {
            term = connect.getTermFrom(date);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return term;
    }

}
