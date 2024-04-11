package edu.cs;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

@WebServlet("/FileUploadServlet")
@MultipartConfig(fileSizeThreshold=1024*1024*10, 	// 10 MB 
               maxFileSize=1024*1024*50,      	// 50 MB
               maxRequestSize=1024*1024*100)   	// 100 MB
public class FileUploadServlet extends HttpServlet {

  private static final long serialVersionUID = 205242440643911308L;
	
  /**
   * Directory where uploaded files will be saved, its relative to
   * the web application directory.
   */
  private static final String UPLOAD_DIR = "uploads";

  private static final String DB_URL = "jdbc:mysql://localhost:3306/db_user";
  private static final String DB_USER = "db_user";
  private static final String DB_PASSWORD = "Thetroller21";

  protected void doPost(HttpServletRequest request,
          HttpServletResponse response) throws ServletException, IOException {
      // gets absolute path of the web application
      String applicationPath = request.getServletContext().getRealPath("");

      // constructs path of the directory to save uploaded file
      String uploadFilePath = applicationPath + File.separator + UPLOAD_DIR;

       
      // creates the save directory if it does not exists
      File fileSaveDir = new File(uploadFilePath);
      if (!fileSaveDir.exists()) {
          fileSaveDir.mkdirs();
      }
      System.out.println("Upload File Directory="+fileSaveDir.getAbsolutePath());
      
      String fileName = "";
      //Get all the parts from request and write it to the file on server
      for (Part part : request.getParts()) {
          fileName = getFileName(part);
          fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
          part.write(uploadFilePath + File.separator + fileName);

       // Insert file metadata into MySQL database
          try {
              Class.forName("com.mysql.cj.jdbc.Driver");
              Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
              String insertQuery = "INSERT INTO uploaded_files (filename, file_path, file_size, file_type) VALUES (?, ?, ?, ?)";
              PreparedStatement pstmt = conn.prepareStatement(insertQuery);
              pstmt.setString(1, fileName);
              pstmt.setString(2, uploadFilePath + File.separator + fileName);
              pstmt.setLong(3, new File(uploadFilePath + File.separator + fileName).length());
              pstmt.setString(4, part.getContentType());
              pstmt.executeUpdate();
              pstmt.close();
              conn.close();
          } catch (ClassNotFoundException | SQLException e) {
              e.printStackTrace();
          }

      }

      String message = "File uploaded successfully!";
      response.getWriter().write(message);
  }

  /**
   * Utility method to get file name from HTTP header content-disposition
   */
  private String getFileName(Part part) {
      String contentDisp = part.getHeader("content-disposition");
      System.out.println("content-disposition header= "+contentDisp);
      String[] tokens = contentDisp.split(";");
      for (String token : tokens) {
          if (token.trim().startsWith("filename")) {
              return token.substring(token.indexOf("=") + 2, token.length()-1);
          }
      }
      return "";
  }
}
