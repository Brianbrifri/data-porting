/*
 *Authors: Jacob Taylor
 */


import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
    final int BEGIN_DATA = 5;
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


                //Student in first cell means that the next row will be the data rea
                if (cell != null && cell.toString().compareTo("Student") == 0) {
                    //make sure no old headers from other files are still around
                    qualityHeaders.clear();

                    //get and store the string headers for each of the quality levels
                    int i = BEGIN_DATA;
                    cell = nextRow.getCell(i);

                    while (cell != null && !cell.toString().isEmpty()) {

                        //map the header to the measurement id as well
                        qualityHeaders.add(mapHeaderToMeasurementID(cell));
                        //System.out.println(i + ": " + cell.toString() + "\n");
                        i++;
                        cell = nextRow.getCell(i);
                    }
                    hasEnteredData = true;
                }
            }
            else
            {
                if(nextRow.cellIterator().hasNext()) {
                    RowToDatabase(nextRow.cellIterator());
                }

            }
        }

    }

    void RowToDatabase (Iterator<Cell> iterator)
    {
        String student = iterator.next().toString();
        String stuID = iterator.next().toString();
        String evaluator = iterator.next().toString();
        String completionDate = iterator.next().toString();
        String isPublished = iterator.next().toString();
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
                measurement = dataToAnchorID(measurement);
            }
            System.out.print(header + ":" + measurement + "\n");
        }

        System.out.println ("");
    }

    //takes in a cell and transforms the data into an Anchor ID
    //throws NoSuchElementException if no element is found
    String dataToAnchorID (String data)
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
            throw new NoSuchElementException ();
        }
    }

    //maps the header to a measurement id in the database
    String mapHeaderToMeasurementID (Cell cell)
    {
        String cell_data = cell.toString().toLowerCase();

        //maps header to quality indicator measurement id
        if (cell_data.contains("quality indicator"))
        {
            //trim the string to get only the numbers needed for quality indicator
            cell_data =  cell_data.replace("quality indicator", "");
            cell_data = cell_data.replace("(levels)", "");
            cell_data = cell_data.replace(" ", "");
            cell_data = cell_data.replace(".", "");
            return "MECE-QI" + cell_data;
        }
        return cell.toString();
    }

}
