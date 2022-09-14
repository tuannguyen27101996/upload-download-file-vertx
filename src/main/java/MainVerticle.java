import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {
    public static final Logger log = org.apache.log4j.Logger.getLogger(MainVerticle.class);
    @Override
    public void start() throws Exception {
        HttpServer server = MainApp.vertx.createHttpServer();
        Router router = Router.router(vertx);

        Route route = router.post("/api/upload")
                .handler(BodyHandler
                        .create()
                        .setUploadsDirectory("upload-folder")
                        .setMergeFormAttributes(true)
                        .setDeleteUploadedFilesOnEnd(true));

        route.handler(ctx -> {
            AtomicReference<List<Response>> responses = new AtomicReference<>();
            List<String> names = new ArrayList<>();
            FileInputStream file = null;
            FileUpload fileUpload = ctx.fileUploads().get(0);
            try {
                file = new FileInputStream(new File( fileUpload.uploadedFileName()));

                //Create Workbook instance holding reference to .xlsx file
                XSSFWorkbook workbook = new XSSFWorkbook(file);

                //Get first/desired sheet from the workbook
                XSSFSheet sheet = workbook.getSheetAt(0);

                //Iterate through each rows one by one
                Iterator<Row> rowIterator = sheet.iterator();
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    //For each row, iterate through all the columns
                    Iterator<Cell> cellIterator = row.cellIterator();
                    while (cellIterator.hasNext()) {
                        Cell cell = cellIterator.next();
                        //Check the cell type and format accordingly
                        if (cell.getColumnIndex() == 1) {
                            names.add(cell.getStringCellValue());
                        }
                    }
                    file.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            names.remove(0);
            vertx.executeBlocking(ft -> {
                responses.set(names.stream().map(ex -> {
                    System.out.println(Thread.currentThread());
                    return new Response("code generated", ex);
                }).collect(Collectors.toList()));

                ft.complete(responses);
            }, res  -> {
                XSSFWorkbook workbook = new XSSFWorkbook();
                AtomicReference<List<Response>> list = (AtomicReference<List<Response>>) res.result();
                //Create a blank sheet
                XSSFSheet sheet = workbook.createSheet("Summary");

                //This data needs to be written (Object[])
                Map<String, Object[]> data = new TreeMap<String, Object[]>();
                data.put("1", new Object[] {"ID", "NAME", "CODE"});
                for (int i = 0; i < list.get().size(); i++) {
                    data.put((i + 2) + "", new Object[] {(i  + 1), list.get().get(i).getName(),list.get().get(i).getCode()});
                }

                //Iterate over data and write to sheet
                Set<String> keyset = data.keySet();
                int rowNum = 0;
                for (String key : keyset)
                {
                    Row row = sheet.createRow(rowNum++);
                    Object [] objArr = data.get(key);
                    int cellNum = 0;
                    for (Object obj : objArr)
                    {
                        Cell cell = row.createCell(cellNum++);
                        if(obj instanceof String)
                            cell.setCellValue((String)obj);
                        else if(obj instanceof Integer)
                            cell.setCellValue((Integer)obj);
                    }
                }

                try
                {
                    //Write the workbook in file system
                    FileOutputStream out = new FileOutputStream(new File("download-folder/response-file.xlsx"));
                    workbook.write(out);
                    out.close();
                    System.out.println("response-file.xlsx written successfully on download-folder.");
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                ctx.response().putHeader(io.vertx.core.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=response-file.xlsx");
                ctx.response().sendFile("download-folder/response-file.xlsx");
            });

        });
        server.requestHandler(router).listen(8080);
    }
}
