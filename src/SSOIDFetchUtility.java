/*
 *Authors: Jacob Taylor
 * Purpose: This creates a utility that allows the person to fetch all the ssoids within the files
 */

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
    private static HashSet<String> ssoidSet = new HashSet<>();

    public static void main(String[] args) throws IOException
    {
        String excelFilePath = "C:/Users/Me/Downloads/PathTest";
        String outputFilePath = "C:/Users/Me/Downloads/TestOutput.txt";

        //all directories that need to be visited
        Stack <File> directoryStack = new Stack <>();
        directoryStack.push (new File (excelFilePath));

        System.out.println ("Starting parsing!");

        //this will go through all possible paths within the directory
        while (!directoryStack.isEmpty())
        {
            //go through each directory and push all directories on the stack or work on the workbooks
            for (File a : directoryStack.pop().listFiles())
            {
                //if it is not a directory then work on it and get ssoids
                if (!a.isDirectory())
                {
                   ReadInWorkbook(a);
                }
                //push it to the stack
                else
                    directoryStack.push(a);

            }
        }

        WriteSetToFile(ssoidSet, new File (outputFilePath));
    }

    private static void WriteSetToFile (Set<String> set, File file)
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

        while (iterator.hasNext())
        {
            try
            {
                bufferedWriter.write(iterator.next());
                bufferedWriter.newLine();
            }catch (IOException e)
            {
                System.out.println (e.toString());
            }
        }

        try
        {
            bufferedWriter.close();
            writer.close();
        }catch (IOException e)
        {
            System.out.println (e.toString());
        }
    }

    private static void ReadInWorkbook (File file)
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
