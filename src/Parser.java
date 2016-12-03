/*
 *Authors: Jacob Taylor
 */


import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.Month;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class Parser
{
    private int BEGIN_DATA = 6;
    private SQLConnect connect = new SQLConnect();
    //this holds the information about what quality level the value belongs to
    private ArrayList<String> qualityHeaders = new ArrayList<>();

    public boolean Parse(String path, StringBuilder usr, StringBuilder pswd) throws IOException
    {
        if(!connect.connect(usr, pswd)) {
            usr.setLength(0);
            pswd.setLength(0);
            return false;
        }
        usr.setLength(0);
        pswd.setLength(0);
        System.out.println ("Reading!");
        File f = new File (path);

        //if it is a directory traverse it else just read in the workbook directly
        if (f.isDirectory())
            TraverseDirectory (f);
        else
        {
            System.out.println("Parsing file");
            if(f.getName().endsWith(".xls")) {
                ReadInWorkbook(f);
            }
        }
        connect.disconnect();
        return true;
    }

    private void TraverseDirectory(File file)
    {
        //all directories that need to be visited
        Stack<File> directoryStack = new Stack<>();
        directoryStack.push(file);

        System.out.println("Starting parsing directory!");

        //this will go through all possible paths within the directory
        while (!directoryStack.isEmpty()) {
            //go through each directory and push all directories on the stack or work on the workbooks
            for (File a : directoryStack.pop().listFiles()) {
                //if it is not a directory then work on it and get ssoids
                if (!a.isDirectory()) {
                    System.out.println("Not dir");
                    if(a.getName().endsWith(".xls")) {
                        System.out.println("Parsing " + a.getName());
                        ReadInWorkbook(a);
                    }

                }
                //push it to the stack
                else
                    directoryStack.push(a);

            }
        }

    }


    private void ReadInWorkbook (File file)
    {
         /*
         * Each workbook is split into two parts! The header which contains information about the assessment
         * and the second part is the data area where all the information is.
         */
        Workbook workbook = null;
        FileInputStream inputStream = null;


        System.out.println("Read in workbook");
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
            System.out.println ("File not found");
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
                        cell = nextRow.cellIterator().next();

                        //try to get the amount id from the 2nd cell in teh evaluation row
                        try
                        {
                            amountID = MapHeaderToAmountID(cell);
                        }catch (NoSuchElementException e)
                        {
                            System.out.println ("Amount ID not able to be parsed!");
                        }

                        //try to get course id from 2nd cell in evaluation row
                        try {

                            courseID = MapHeaderToCourseID(cell);
                        } catch (NoSuchElementException e)
                        {
                            System.out.println("Course ID not able to be parsed!");
                        }

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

    private void RowToDatabase (Iterator<Cell> iterator, String amountID, String courseID)
    {
        String student = iterator.next().toString();
        String stuID = RemoveScientificNotation(getStudentId(iterator.next().toString()));
        String evaluator = iterator.next().toString();
        String completionDate = FormatDate(iterator.next().toString());
        String isPublished = iterator.next().toString();
        String completionStatus = iterator.next().toString();

        Cell cell;


        String measurement;
        //pair quality header with actual amounts
        for (String header : qualityHeaders)
        {
            cell = iterator.next();
            measurement = cell.toString();

            //if it is a quality indicator then convert it to anchor id
            if (header.contains("MECE-QI"))
            {
                measurement = DataToAnchorID(measurement);
                System.out.println (GenerateSQLQuery("observationID", GenerateAssessmentTrialID(stuID, amountID, "4", "000", convertCompletionDateToTerm(completionDate)), evaluator, "RATER", measurement, header, null, completionDate));
            }
            if (header.contains("MOTS-QIW1")) {
                measurement = writingQualityToId(measurement);
                System.out.println (GenerateSQLQuery("observationID", GenerateAssessmentTrialID(stuID, amountID, "4", "000", convertCompletionDateToTerm(completionDate)), evaluator, "RATER", measurement, header, null, completionDate));
            }
        }
    }

    //Checks if External ID is a number or not. If not,
    //looks up the string in the table SSOID_MAPPINGS
    //and returns the correct EMPLID. Returns 000000000 otherwise
    private String getStudentId(String stuID) {
        boolean isNumber = true;
        try {
            //noinspection ResultOfMethodCallIgnored
            Float.parseFloat(stuID);
        } catch (NumberFormatException e) {
            isNumber = false;
        }
        if(isNumber) {
            return stuID;
        }
        else {
            try {
                return connect.getEmplidMappingFrom(stuID);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return "00000000";
    }
    //takes in a cell and transforms the data into an Anchor ID
    //throws NoSuchElementException if no element is found
   private String DataToAnchorID(String data)
    {
        data = data.toLowerCase();

        if (data.contains("emerging"))
        {
            return "MOTS-CLEV-EME1";
        } else if (data.contains("n/a"))
        {
            return "MOTS-CLEV-NA";
        } else if (data.contains("candidate"))
        {
            return "MOTS-CLEV-CAND";
        } else if (data.contains("below"))
        {
            return "MOTS-CLEV-BELO";
        } else if (data.contains("developing"))
        {
            return "MOTS-CLEV-DEVE";
        }
        else if (data.contains("revision")) {
            return "MOTS-CLEV-NA";
        }
        else
        {
            //return N/A for any other value
            //this may not be the correct approach
            return "MOTS-CLEV-NA";
        }
    }

    //Calls the SQLConnect function to look up matching
    //ANCHOR_ID to the measurement
    private String writingQualityToId(String data) {
        data = data.toLowerCase();
        String anchorID = null;
        try {
            anchorID = connect.getWritingQualityFrom(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return anchorID;
    }

    //maps the header to a measurement id in the database
   private String MapHeaderToMeasurementID(Cell cell)
    {
        String cell_data = cell.toString().toLowerCase();
        StringBuilder builder = new StringBuilder();

        //maps header to quality indicator measurement id
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
        else if (cell_data.contains("writing quality")) {
            return "MOTS-QIW1";
        }
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
            return string;
        }
        else if (string.contains("p1"))
        {
            string = "MEES-CEFO-V001";
            return string;
        }
        else throw new NoSuchElementException();
    }

    //gets information about string values and uses them to find the course
    private String MapHeaderToCourseID (Cell cell) throws NoSuchElementException
    {
        String string = cell.toString().toLowerCase();

        if (string.contains("health and pe"))
        {
            return "health and pe";
        }
        else if (string.contains ("music"))
        {
            return "music";
        }
        else throw new NoSuchElementException();
    }

    private String GenerateAssessmentTrialID (String candidateID,String amountID, String courseID, String classSection, String term)
    {
        return candidateID + "-" + amountID + "-" + courseID + "-" + classSection + "-" + term;
    }

    private String GenerateSQLQuery (String obsID, String trialID, String empID, String actorType, String anchorID, String measurementID, String response, String observationDate)
    {


        try {
            connect.insertObservationWith(trialID, empID, actorType, anchorID, measurementID, response, observationDate);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "INSERT INTO P2_EQS_OBS (\"TRIAL_ID\", \"EMPLID\", \"ACTOR_TYPE\", \"ANCHOR_ID\", \"MMNT_ID\", \"TEXT_RESPONSE\", \"OBS_DT\")" +
                "VALUES ('"+ trialID + "', '"+ empID +"', '"+actorType+"', '" + anchorID +"', '"+ measurementID +"', '" + response +"', '" + observationDate +"');";
    }

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
        StringBuilder builder = new StringBuilder(date);
        System.out.println (date);
        String month = MonthToDigits (date.substring(3,6));
        String day = date.substring(0, 2);
        String year = date.substring(7, 11);
        return year + "-" + month + "-" + day;
    }

    //returns a month as digits instead of abbreviations
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
