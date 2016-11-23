/*
 *Authors: Jacob Taylor
 */


import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Stack;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class Parser
{

    public void Parse(String path) throws IOException
    {
        System.out.println ("Reading!");
        File f = new File (path);

        //if it is a directory traverse it else just read in the workbook directly
        if (f.isDirectory())
            TraverseDirectory (f);
        else
        {
            System.out.println ("Parsing file");
            ReadInWorkbook(f);
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

        Iterator<Row> iterator = firstSheet.iterator();


        //work on the the cells within the document
        while (iterator.hasNext())
        {
            Row nextRow = iterator.next();

            //look for the start of the data area
            if (!hasEnteredData)
            {
                Cell cell = nextRow.cellIterator().next();

                //Student in first cell means that the next row will be the data rea
                if (cell.toString().compareTo("Student") == 0)
                {
                    hasEnteredData = true;
                }
            }
            else
            {
                RowToDatabase(nextRow.cellIterator());
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
        System.out.println (isPublished);

        Cell cell;
        
        //now get information on quality levels
        while (iterator.hasNext())
        {
            cell = iterator.next();

            if (!cell.toString().isEmpty())
                System.out.print (cell.toString() + ", ");
        }

        System.out.println ("");
    }

}
