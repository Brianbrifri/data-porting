/*
 *Authors: Jacob Taylor
 * Purpose: This creates a utility that allows the person to fetch all the ssoids within the files
 */

import jdk.nashorn.internal.runtime.regexp.joni.constants.Traverse;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.*;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class SSOIDFetchUtility
{
    //set of all ssoids that does not contain any duplicates
    private Set<String> ssoidSet = new HashSet<>();
    String excelFilePath = "/home/b-kizzle/Downloads";
    String outputFilePath = "/home/b-kizzle/TestOutput.txt";
    public void run() throws IOException
    {

        File f  = new File (excelFilePath);

        //if it is a directory traverse it else just read in the workbook directly
        if (f.isDirectory())
            TraverseDirectory (f);
        else
        {
            System.out.println ("Parsing file");
            ReadInWorkbook(f);
        }

        WriteSetToFile(ssoidSet, new File (outputFilePath));
    }

    private void TraverseDirectory(File file)
    {
        //all directories that need to be visited
        Stack<File> directoryStack = new Stack<>();
        directoryStack.push(file);

        System.out.println("Starting parsing directory!");

        //this will go through all possible paths within the directory
        while (!directoryStack.isEmpty())
        {
            //go through each directory and push all directories on the stack or work on the workbooks
            for (File a : directoryStack.pop().listFiles())
            {
                //if it is not a directory then work on it and get ssoids
                if (!a.isDirectory())
                {
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

    private void WriteSetToFile (Set<String> set, File file)
    {
        FileWriter writer = null;
        BufferedWriter bufferedWriter = null;

        try
        {
            writer = new FileWriter(file);
            bufferedWriter = new BufferedWriter(writer);
        } catch (IOException e)
        {
            System.out.println (e.toString());
        }

        Iterator<String> iterator = set.iterator();

        while (iterator.hasNext()) {
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.write(iterator.next());
                    bufferedWriter.newLine();
                }

            } catch (IOException e) {
                System.out.println(e.toString());
            }
        }

        try
        {
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (writer != null) {
                writer.close();
            }
        }catch (IOException e)
        {
            System.out.println (e.toString());
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

        //flag to check if the program is in the data area
        boolean hasEnteredData = false;

        try
        {
            inputStream = new FileInputStream(file);
            workbook = new HSSFWorkbook(inputStream);
        }
        catch (IOException e)
        {
            System.out.println (e.toString());
        }

        Sheet firstSheet = null;
        try
        {
            if (workbook != null) {
                firstSheet = workbook.getSheetAt(0);
            }
        }catch (NullPointerException e)
        {
            System.out.println (e.toString());
        }

        Iterator<Row> iterator = null;
        if (firstSheet != null) {
            iterator = firstSheet.iterator();
        }

        int columnPosition;

        //work on the the cells within the document
        if (iterator != null) {
            while (iterator.hasNext()) {
                Row nextRow = iterator.next();
                Iterator<Cell> cellIterator = nextRow.cellIterator();



                columnPosition = 0;
                //go through all the cells in each row
                while (cellIterator.hasNext())
                {
                    Cell cell = cellIterator.next();


                    //if the id index is -1 it means that the program has not come accross the correct info
                    if (idIndex  == -1)
                    {
                        //this means we have not come accrossed the header for the data area yet
                        if (cell.getStringCellValue().compareToIgnoreCase("student") != 0 && !hasEnteredData)
                        {
                            //therefore just break out of loop to go to next row
                            break;
                        }
                        //now entering data area copy the column indexes for each type
                        else
                        {
                            hasEnteredData = true;

                            //copy all indexes down  by checking for keywords in header like ID or date
                            if (cell.getStringCellValue().contains("ID"))
                                idIndex =  columnPosition;
                        }
                    }
                    //we are storing the data
                    else
                    {
                        //add the id to the set if it is a ssoid which cant be parsed to an int
                        if (columnPosition == idIndex)
                        {
                            try
                            {
                                //if this can be parsed as a float then that means its an a student id
                                //not an ssoid
                                Float.parseFloat(cell.toString());
                            }
                            //this means that the id was a ssiod so add it to the set of them that needs to be put into a file
                            catch (NumberFormatException e)
                            {
                                ssoidSet.add(cell.toString());
                            }
                            //go ahead and break out of the while loop and go to the next row
                            break;
                        }
                    }

                    columnPosition++;
                }

                try
                {
                    inputStream.close();
                }catch (IOException e)
                {
                    System.out.println (e.toString());
                }
            }
        }

    }

}
