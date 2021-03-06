package bsu.fpmi.chat.controller;

import bsu.fpmi.chat.dao.MessageDaoImpl;
import bsu.fpmi.chat.util.ServletUtil;
import bsu.fpmi.chat.xml.XMLHistoryChange;
import bsu.fpmi.chat.xml.XMLHistoryUtil;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.xml.sax.SAXException;

import org.apache.log4j.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.UUID;

import static bsu.fpmi.chat.util.MessageUtil.*;

@WebServlet(urlPatterns = {"/chat"}, asyncSupported = true)
public class TaskServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static Logger logger = Logger.getLogger(TaskServlet.class.getName());
    private static MessageDaoImpl messageDaoImpl = new MessageDaoImpl();

    @Override
    public void init() throws ServletException {
        try {
            loadHistory();
        } catch (SAXException | IOException | ParserConfigurationException | TransformerException e) {
            logger.error(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        String token = request.getParameter(TOKEN);
//        logger.info("doGet");
//        String data = ServletUtil.getMessageBody(request);
//        logger.info(data);
//        try {
//            if (token != null && !"".equals(token)) {
//                int index = getIndex(token);
//                logger.info("Index " + index);
//                String messages = XMLHistoryUtil.getMessages(index);
//                response.setContentType(ServletUtil.APPLICATION_JSON);
//                response.setCharacterEncoding("UTF-8");
//                PrintWriter out = response.getWriter();
//                out.print(messages);
//                out.flush();
//            } else {
//                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "'token' parameter needed");
//            }
//        } catch (SAXException | IOException | ParserConfigurationException | XPathExpressionException e) {
//
//        }
        final AsyncContext asyncContext = request.startAsync();
        logger.info("doGet");
        String data = "Token: "+request.getParameter(TOKEN);
        logger.info(data);

        AsynchronousProcessor.addAsyncContext(asyncContext);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.info("doPost");
        String data = ServletUtil.getMessageBody(request);
        logger.info("Message body: " + data);
        try {
            JSONObject message = stringToJson(data);
            message.put(ID, UUID.randomUUID().toString());
            XMLHistoryUtil.addData(message);
            //
            messageDaoImpl.add(message);
            //
            AsynchronousProcessor.notifyAllClients();
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (ParseException | ParserConfigurationException | SAXException | TransformerException e) {
            logger.error(e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.info("doPut");
        String data =ServletUtil.getMessageBody(request);
        logger.info("Message body: "+data);
        try {
            JSONObject message = stringToJson(data);
            if (message != null) {
                XMLHistoryUtil.updateData(message);
                //
                messageDaoImpl.update(message);
                //
                AsynchronousProcessor.notifyAllClients();
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Task does not exist");
            }
        } catch (ParseException | ParserConfigurationException | SAXException | TransformerException | XPathExpressionException e) {
            logger.error(e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        logger.info("doDelete");
        logger.info("Id: "+request.getParameter(ID));
        String id = request.getParameter(ID);
        try {
            XMLHistoryUtil.deleteData(id);
            //
            messageDaoImpl.delete(id);
            //
                AsynchronousProcessor.notifyAllClients();
                response.setStatus(HttpServletResponse.SC_OK);

               // response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Task does not exist");
        } catch (ParserConfigurationException | SAXException | TransformerException | XPathExpressionException e) {
            logger.error(e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void loadHistory() throws SAXException, IOException, ParserConfigurationException, TransformerException {
        if (!XMLHistoryUtil.doesStorageExist()) {
            XMLHistoryUtil.createStorage();
        }
        if (!XMLHistoryChange.doesStorageExist()) {
            XMLHistoryChange.createStorage();
        }
    }
}
