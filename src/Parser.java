/*
 *Authors: Jacob Taylor
 */


import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class Parser
{
    final int BEGIN_DATA = 6;
    //this holds the information about what quality level the value belongs to
    private ArrayList<String> qualityHeaders = new ArrayList<>();

    public void Parse(String path) throws IOException
    {
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
                    if(a.getName().endsWith(".xls")) {
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


        //flag to check if the program is in the data area
        boolean hasEnteredData = false;
        //the type of assesment like summative or formative
        String amountID = "";


        try
        {
            inputStream = new FileInputStream(file);
            workbook = new HSSFWorkbook(inputStream);
            System.out.println ("Done this");
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
                if (cell != null && cell.toString().compareTo("Student") == 0)
                {
                    //make sure no old headers from other files are still around
                    qualityHeaders.clear();

                    //the qualityHeaders start at BEGIN_DATA
                    int i = BEGIN_DATA;

                    //before the quality headers is information about completed use this to get amountID
                    amountID = MapHeaderToAmountID(nextRow.getCell(i - 1));
                    System.out.println (amountID);
                    cell = nextRow.getCell(i);

                    //get and store the string headers for each of the quality levels
                    while (cell != null && !cell.toString().isEmpty())
                    {

                        //map the header to the measurement id as well
                        qualityHeaders.add(MapHeaderToMeasurementID(cell));
                        //System.out.println(i + ": " + cell.toString() + "\n");
                        i++;
                        cell = nextRow.getCell(i);
                    }
                    hasEnteredData = true;
                }
            }
            else
            {
                if(nextRow.cellIterator().hasNext())
                {
                    RowToDatabase(nextRow.cellIterator(), amountID);
                }

            }
        }

    }

    private void RowToDatabase (Iterator<Cell> iterator, String amountID)
    {
        String student = iterator.next().toString();
        String stuID = RemoveScientificNotation(iterator.next().toString());
        String evaluator = iterator.next().toString();
        String completionDate = FormatDate(iterator.next().toString());
        String isPublished = iterator.next().toString();
        String completionStatus = iterator.next().toString();

        Cell cell;


        String measurement = "";
        //pair quality header with actual amounts
        for (String header : qualityHeaders)
        {
            cell = iterator.next();
            measurement = cell.toString();

            //if it is a quality indicator then convert it to anchor id
            if (header.contains("MECE-QI"))
            {
                measurement = DataToAnchorID(measurement);
                System.out.println (GenerateSQLQuery("observationID", GenerateAssessmentTrialID(stuID, amountID, "4", "4", "4"), stuID, "RATER", measurement, header, null, completionDate));
            }
        }
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
        else
        {
            //return N/A for any other value
            //this may not be the correct approach
            return "MOTS-CLEV-NA";
        }
    }

    //maps the header to a measurement id in the database
   private String MapHeaderToMeasurementID(Cell cell)
    {
        String cell_data = cell.toString().toLowerCase();

        //maps header to quality indicator measurement id
        if (cell_data.contains("quality indicator"))
        {

            char[] digits = new char[2];
            int index = 0;

            //go through character by character to find the numbers for quality indicator
            for (char c : cell_data.toCharArray())
            {
                //if a character is a digit add it to digits for QI
                if (c >= '0' && c <= '9')
                {
                    digits [index] = c;
                    index++;
                }
            }

            return "MECE-QI" + String.copyValueOf(digits);
        }
        return cell.toString();
    }

    //maps the header for the completion of a evalution to a amount id
    //p1 being formative and p2 being summative
    private String MapHeaderToAmountID(Cell cell)
    {
        String string = cell.toString().toLowerCase();

        if (string.contains("p2") || string.contains("practicum 2"))
        {
            string = "MEES-CESU-V001";
        }
        else if (string.contains("p1"))
        {
            string = "MEES-CEFO-V001";
        }
        return string;
    }

    private String GenerateAssessmentTrialID (String candidateID,String amountID, String courseID, String classSection, String term)
    {
        return candidateID + "-" + amountID + "-" + courseID + "-" + classSection + "-" + term;
    }

    private String GenerateSQLQuery (String obsID, String trialID, String empID, String actorType, String anchorID, String measurementID, String response, String observationDate)
    {
        return "INSERT INTO \"COE\".\"P2_EQS_OBS\" (\"OBS_ID\", \"TRIAL_ID\", \"EMPLID\", \"ACTOR_TYPE\", \"ANCHOR_ID\", \"MMNT_ID\", \"TEXT_RESPONSE\", \"OBS_DT\")" +
                "VALUES ('" + obsID +"', '"+ trialID + "', '"+ empID +"', '"+actorType+"', '" + anchorID +"', '"+ measurementID +"', " + response +", TIMESTAMP '" + observationDate +"');";
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

        return string;
    }

    //this puts the date in the correct format recognized by the database
    private String FormatDate (String date)
    {
        System.out.println (date);
        return date;
    }

}
