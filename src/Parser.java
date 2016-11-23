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

        //these are indexes that specify what column each type of data is in in the data area
        int idIndex = -1;
        int dateIndex = -1;

        //these stacks will store the specific information
        //this is most likely temp
        Stack<String> idStack = new Stack <>();
        Stack<String> dateStack = new Stack <>();

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

        int columnPosition;

        //work on the the cells within the document
        while (iterator.hasNext()) {
            Row nextRow = iterator.next();
            Iterator<Cell> cellIterator = nextRow.cellIterator();



            columnPosition = 0;
            //go through all the cells in each row
            while (cellIterator.hasNext())
            {
                Cell cell = cellIterator.next();


                //if the id index is -1 it means that the program has not come accross the correct info
                if (idIndex  == -1 || dateIndex == -1)
                {
                    //this means we have not come accrossed the header for the data area yet
                    if (cell.getStringCellValue().compareToIgnoreCase("student") != 0 && !hasEnteredData)
                    {
                        //therefore just break out of loop to go to next row
                        System.out.println(cell.getStringCellValue());
                        break;
                    }
                    //now entering data area copy the column indexes for each type
                    else
                    {
                        System.out.println (cell.toString());
                        hasEnteredData = true;

                        //copy all indexes down  by checking for keywords in header like ID or date
                       if (cell.getStringCellValue().contains("ID"))
                           idIndex =  columnPosition;
                        else if (cell.getStringCellValue().contains("Date"))
                            dateIndex = columnPosition;
                    }
                }
                //we are storing the data
                else
                {
                    if (columnPosition == idIndex)
                    {
                        System.out.println ("Push to id");
                        idStack.push(cell.toString());
                    }
                    else if (columnPosition == dateIndex)
                    {
                        System.out.println ("Push to date");
                        dateStack.push(cell.toString());
                    }
                }

                columnPosition++;
            }
        }


        //popping all stacks just to show the correct info in console
        while (!idStack.isEmpty() || !dateStack.isEmpty())
        {
            System.out.println(idStack.pop());
            System.out.println (dateStack.pop());
        }
        try
        {
            workbook.close();
            inputStream.close();
        }catch (Exception e)
        {
            System.out.println (e.toString());
        }

    }

}
