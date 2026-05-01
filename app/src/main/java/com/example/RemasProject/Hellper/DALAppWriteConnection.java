/**
 * هاد الملف كل الشغل الثقيل مع Appwrite — جداول، مستندات، رفع صور، تنزيل بروابط؛ الباقي الشاشات بتستدعي دوال جاهزة من هون.
 */
package com.example.RemasProject.Hellper;

import android.content.Context;
import android.util.Log;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import com.example.RemasProject.model.LetterListenRecord;
import com.example.RemasProject.model.LetterQuizSession;
import com.example.RemasProject.model.StudentProgress;

public class DALAppWriteConnection {
    
    /** للوجات القصيرة في Logcat */
    private static final String LOG_TAG = "RemasDB";
    
    // === إعدادات الاتصال مع Appwrite ===
    private static final String BASE_URL = "https://fra.cloud.appwrite.io/v1";
    private static final String PROJECT_ID = "690734060019d047f2e4";
    private static final String API_KEY = "standard_537269c91e4dae90604cab81befa943c70ee375a7b0f4cfc424f83f15a264cf9ceefe2d8b7f00588daddb40808636b93254e052dabfc6d820d28e9d35b8397770a62aabd805c83a2ef2ce312d1db2f58eb66cdad160af6d1dfa2500ce4afc827cd481224b48213cb5d387c3c7529af2a3f39792fd4f3e02396b18a5b31a23f21";
    private static final String MAIN_DATABASE_ID = "690c672100204fe70734"; // AppDb
    private static final String MAIN_STORAGE_BUCKET_ID = "690c67460024eed73cc2"; // AppWriteStorage
    
    private Context context;
    private Gson gson;
    
    /**
     * منشئ الكلاس الرئيسي
     * @param context تطبيق أندرويد للحصول على الأذونات والموارد
     */
    public DALAppWriteConnection(Context context) {
        this.context = context;
        this.gson = new Gson();
        
    }
    
    // === نماذج البيانات الأساسية ===
    
    /**
     * نموذج نتيجة العملية
     */
    public static class OperationResult<T> {
        public boolean success;
        public String message;
        public T data;
        public String errorCode;
        
        public OperationResult() {}
        
        public OperationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public OperationResult(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }
    }
    
    /**
     * نموذج الملف المرفوع
     */
    public static class FileInfo {
        public String fileId;
        public String fileName;
        public String fileUrl;
        public String mimeType;
        public long fileSize;
        public String uploadedBy;
        public java.util.Date uploadDate;
        public String bucketId;
        
        public FileInfo() {}
    }
    
    /**
     * قراءة الرسالة الخطأ من استجابة Appwrite
     * @param connection اتصال HTTP
     * @return رسالة الخطأ أو "فشل غير محدد"
     */
    private String readErrorResponse(HttpURLConnection connection) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            StringBuilder errorMessage = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                errorMessage.append(line);
            }
            reader.close();
            return errorMessage.toString();
        } catch (Exception e) {
            return "فشل غير محدد";
        }
    }
    
    /**
     * استخراج قيمة من سلسلة الاستجابة JSON
     * @param responseString سلسلة الاستجابة
     * @param key المفتاح المراد استخراج قيمته
     * @return قيمة المفتاح أو null إذا لم يكن موجوداً
     */
    private String extractValue(String responseString, String key) {
        try {
            // تحليل السلسلة الأولية
            String[] parts = responseString.split("\"");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals(key)) {
                    return parts[i + 2]; // القيمة هي بعد المفتاح والعلامة "
                }
            }
        } catch (Exception e) {
        }
        return null;
    }
    
    // === دوال إدارة قواعد البيانات العامة ===
    
    /**
     * حفظ كائن أو مجموعة كائنات في قاعدة البيانات
     * إذا كان الجدول غير موجود، سيتم إنشاؤه تلقائياً
     * إذا كان موجوداً، سيتم إضافة البيانات إليه
     * 
     * @param data البيانات المراد حفظها (كائن واحد أو ArrayList)
     * @param tableName اسم الجدول
     * @param collectionId معرف المجموعة (اختياري)
     * @return نتيجة العملية مع قائمة الكائنات المحفوظة
     * 
     * مثال على الاستخدام:
     * // حفظ كائن واحد
     * Product product = new Product();
     * product.name = "هاتف ذكي";
     * product.price = 999.99;
     * 
     * OperationResult<ArrayList<Product>> result = dal.saveData(product, "products");
     * 
     * if (result.success) {
     *     Log.d("SUCCESS", "تم حفظ " + result.data.size() + " منتج");
     * } else {
     *     Log.e("ERROR", "فشل حفظ البيانات: " + result.message);
     * }
     * 
     * // حفظ مجموعة كائنات
     * ArrayList<Product> products = new ArrayList<>();
     * products.add(product1);
     * products.add(product2);
     * 
     * OperationResult<ArrayList<Product>> bulkResult = dal.saveData(products, "products");
     * 
     * if (bulkResult.success) {
     *     Log.d("BULK", "تم حفظ " + bulkResult.data.size() + " منتج");
     * }
     */
    public <T> OperationResult<ArrayList<T>> saveData(T data, String tableName, String collectionId) {
        try {
            // مسار عام: تحويل المدخل لقائمة → التأكد من وجود collection في Appwrite (أو إنشاؤه بـ schema مُستنتج) → POST مستند لكل عنصر
            // التحقق من صحة البيانات
            if (data == null) {
                return new OperationResult<>(false, "البيانات المراد حفظها لا يمكن أن تكون فارغة");
            }
            
            // معالجة الكائن الواحد
            ArrayList<T> dataList;
            if (!(data instanceof Collection)) {
                dataList = new ArrayList<>();
                dataList.add(data);
            } else {
                @SuppressWarnings("unchecked")
                ArrayList<T> castedList = (ArrayList<T>) data;
                dataList = castedList;
            }
            
            if (dataList.isEmpty()) {
                return new OperationResult<>(false, "قائمة البيانات فارغة");
            }
            
            // إنشاء الجدول إذا لم يكن موجوداً
            // إذا كان الجدول موجوداً مسبقاً، سيتم استخدامه مباشرة
            String expectedCollectionName = collectionId != null ? collectionId : tableName;
            
            // الحصول على الـ collection ID الفعلي
            String actualCollectionId = getActualCollectionId(tableName, collectionId);
            if (actualCollectionId == null) {
                actualCollectionId = expectedCollectionName;
            }
            
            
            // فقط إذا لم يكن موجوداً، سنحاول إنشاؤه
            if (!tableExists(tableName, collectionId)) {
                
                // استنتاج schema تلقائياً من الكائن الأول
                String schema = null;
                if (!dataList.isEmpty()) {
                    schema = inferSchemaFromObject(dataList.get(0));
                }
                
                boolean tableCreated = ensureTableExists(tableName, collectionId, schema); // مع schema
                if (!tableCreated) {
                    return new OperationResult<>(false, "فشل في إنشاء الجدول: " + tableName);
                }
                
                // تحديث الـ collection ID بعد الإنشاء
                actualCollectionId = getActualCollectionId(tableName, collectionId);
                if (actualCollectionId == null) {
                    actualCollectionId = expectedCollectionName;
                }
            } else {
                
                // التحقق من وجود attributes - إذا كانت فارغة، أضف schema تلقائياً
                String schema = null;
                if (!dataList.isEmpty()) {
                    schema = inferSchemaFromObject(dataList.get(0));
                    createTableAttributes(actualCollectionId, schema, tableName);
                }
            }
            
            
            ArrayList<T> savedItems = new ArrayList<>();
            int successCount = 0;
            
            // حفظ كل عنصر
            for (T item : dataList) {
                try {
                    // إنشاء مستند جديد أو تحديث موجود
                    String documentId = getObjectId(item);
                    final boolean hadPersistedId = documentId != null && !documentId.isEmpty();
                    if (!hadPersistedId) {
                        documentId = UUID.randomUUID().toString();
                    }
                    
                    Map<String, Object> documentData = convertObjectToMap(item);
                    
                    // تنظيف البيانات من الحقول غير المرغوب فيها
                    documentData.remove("documentId");
                    documentData.remove("createdAt");
                    documentData.remove("createdBy");
                    documentData.remove("class"); // إزالة أي حقول من Java
                    documentData.remove("$"); // إزالة أي حقول خاصة
                    documentData.remove("metadata"); // إزالة metadata - يسبب مشاكل
                    
                    // تحويل "id" إلى اسم فريد حسب نوع الكائن
                    if (documentData.containsKey("id")) {
                        String className = item.getClass().getSimpleName().toLowerCase();
                        String newIdKey = className + "Id";
                        documentData.put(newIdKey, documentData.get("id"));
                        documentData.remove("id");
                    }

                    if (item instanceof StudentProgress) {
                        encodeStudentProgressMapsForWrite(documentData, (StudentProgress) item);
                        // مخطط Appwrite يتطلّب غالباً studentprogressId؛ عند مستند جديد لا يوجد id في الـ Map فيُستبدل هنا بمعرّف المستند.
                        ensureStudentProgressStudentprogressId(documentData, documentId);
                    }
                    
                    // ملاحظة: لا نضيف metadata لأن Appwrite يتطلب تعريف الحقول مسبقاً
                    // يجب إضافة الحقول من لوحة التحكم Appwrite Console
                    
                    boolean saved;
                    if (hadPersistedId) {
                        saved = patchDocument(tableName, actualCollectionId, documentId, documentData);
                        if (!saved) {
                            saved = saveDocument(tableName, actualCollectionId, documentId, documentData);
                        }
                    } else {
                        saved = saveDocument(tableName, actualCollectionId, documentId, documentData);
                    }
                    if (saved) {
                        // تعيين ID في الكائن بعد الحفظ
                        try {
                            java.lang.reflect.Method setIdMethod = item.getClass().getMethod("setId", String.class);
                            setIdMethod.invoke(item, documentId);
                        } catch (Exception e) {
                            // لا توجد setter للـ id، تجاهل
                        }
                        
                        savedItems.add(item);
                        successCount++;
                    } else {
                        Log.e(LOG_TAG, "saveData: فشل saveDocument لعنصر " + item.getClass().getSimpleName());
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "saveData: استثناء أثناء حفظ عنصر", e);
                }
            }
            
            if (successCount > 0) {
                String message = "تم حفظ " + successCount + " عنصر بنجاح من أصل " + dataList.size();
                return new OperationResult<>(true, message, savedItems);
            } else {
                return new OperationResult<>(false, "فشل في حفظ جميع العناصر");
            }
            
        } catch (Exception e) {
            return new OperationResult<>(false, "خطأ في حفظ البيانات: " + e.getMessage());
        }
    }
    
    /**
     * جلب البيانات من جدول محدد
     * @param tableName اسم الجدول
     * @param collectionId معرف المجموعة (اختياري)
     * @param classType نوع الكلاس المطلوب تحويل البيانات إليه
     * @return نتيجة العملية مع قائمة البيانات
     * 
     * مثال على الاستخدام:
     * // جلب المنتجات
     * OperationResult<ArrayList<Product>> result = dal.getData("products", null, Product.class);
     * 
     * if (result.success) {
     *     ArrayList<Product> products = result.data;
     *     Log.d("PRODUCTS", "تم جلب " + products.size() + " منتج");
     *     
     *     for (Product product : products) {
     *         Log.d("PRODUCT", "الاسم: " + product.name + ", السعر: " + product.price);
     *     }
     * } else {
     *     Log.e("ERROR", "فشل جلب البيانات: " + result.message);
     * }
     */
    public <T> OperationResult<ArrayList<T>> getData(String tableName, String collectionId, Class<T> classType) {
        try {
            // التحقق من وجود الجدول
            if (!tableExists(tableName, collectionId)) {
                return new OperationResult<>(false, "الجدول غير موجود: " + tableName);
            }
            
            URL url = new URL(BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/" + 
                            (collectionId != null ? collectionId : tableName) + "/documents");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                // تحليل الاستجابة وتحويلها لكائنات Java
                ArrayList<T> items = parseDocumentResponse(response.toString(), classType);
                
                return new OperationResult<>(true, "تم جلب البيانات بنجاح", items);
                
            } else {
                String errorMessage = readErrorResponse(connection);
                Log.e(LOG_TAG, "getData فشل collection=" + tableName + " code=" + responseCode + " → " + errorMessage);
                return new OperationResult<>(false, "فشل جلب البيانات: " + errorMessage);
            }
            
        } catch (Exception e) {
            Log.e(LOG_TAG, "getData استثناء collection=" + tableName, e);
            return new OperationResult<>(false, "خطأ في جلب البيانات: " + e.getMessage());
        }
    }

    /**
     * استعلام equal بتنسيق JSON كما يتوقعه Appwrite 1.9+ في REST (الصيغة المختصرة equal(...) تُرفض).
     * يطابق تسلسل {@code Query.equal} في SDK: الحقل {@code attribute} وليس {@code column} (الأخير لـ Tables DB).
     */
    private static String appwriteQueryEqual(String attribute, String value) {
        JsonObject q = new JsonObject();
        q.addProperty("method", "equal");
        q.addProperty("attribute", attribute);
        JsonArray values = new JsonArray();
        values.add(value != null ? value : "");
        q.add("values", values);
        return q.toString();
    }

    private static String appwriteQueryLimit(int limit) {
        JsonObject q = new JsonObject();
        q.addProperty("method", "limit");
        JsonArray values = new JsonArray();
        values.add(limit);
        q.add("values", values);
        return q.toString();
    }

    /**
     * جلب مستندات مع queries (مثل equal + limit). بدون ذلك قد يعيد Appwrite أول صفحة فقط
     * فيُعرض تقدّم طالب قديم أو يُفقد مستند {@code letter_listens} في الواجهة.
     */
    public <T> OperationResult<ArrayList<T>> getDataWithQueries(
            String tableName,
            String collectionId,
            Class<T> classType,
            List<String> queries) {
        try {
            if (!tableExists(tableName, collectionId)) {
                return new OperationResult<>(false, "الجدول غير موجود: " + tableName);
            }
            String coll = collectionId != null ? collectionId : tableName;
            StringBuilder urlSb = new StringBuilder(BASE_URL + "/databases/" + MAIN_DATABASE_ID
                    + "/collections/" + coll + "/documents");
            if (queries != null && !queries.isEmpty()) {
                urlSb.append("?");
                for (int i = 0; i < queries.size(); i++) {
                    if (i > 0) {
                        urlSb.append("&");
                    }
                    urlSb.append("queries[]=").append(
                            URLEncoder.encode(queries.get(i), StandardCharsets.UTF_8.name()));
                }
            }
            URL url = new URL(urlSb.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                ArrayList<T> items = parseDocumentResponse(response.toString(), classType);
                return new OperationResult<>(true, "تم جلب البيانات بنجاح", items);
            } else {
                String errorMessage = readErrorResponse(connection);
                Log.e(LOG_TAG, "getDataWithQueries فشل collection=" + tableName
                        + " code=" + responseCode + " → " + errorMessage);
                return new OperationResult<>(false, "فشل جلب البيانات: " + errorMessage);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "getDataWithQueries استثناء collection=" + tableName, e);
            return new OperationResult<>(false, "خطأ في جلب البيانات: " + e.getMessage());
        }
    }

    /**
     * عند وجود أكثر من مستند {@code student_progress} لنفس الطالب نأخذ الأحدث تحديثاً.
     */
    public static StudentProgress pickNewestStudentProgress(List<StudentProgress> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        StudentProgress best = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            StudentProgress p = list.get(i);
            Date bu = best.getUpdatedAt();
            Date pu = p.getUpdatedAt();
            if (pu != null && (bu == null || pu.after(bu))) {
                best = p;
            }
        }
        return best;
    }

    /** سجلات الاستماع الخاصة بالطالب فقط (مع استعلام equal؛ احتياطي: تصفية محلية). */
    public OperationResult<ArrayList<LetterListenRecord>> getLetterListenRecordsForStudent(String studentId) {
        if (studentId == null || studentId.isEmpty()) {
            return new OperationResult<>(false, "معرف الطالب فارغ", new ArrayList<>());
        }
        List<String> queries = new ArrayList<>();
        queries.add(appwriteQueryEqual("studentRef", studentId));
        queries.add(appwriteQueryLimit(500));
        OperationResult<ArrayList<LetterListenRecord>> r =
                getDataWithQueries(LetterListenRecord.COLLECTION_NAME, null,
                        LetterListenRecord.class, queries);
        if (r.success && r.data != null) {
            return r;
        }
        // احتياط: إذا رفض السيرفر صيغة queries[] نجمع كل السجلات ثم نمرّر ما يطابق studentRef فقط (أغلى لكن يعمل)
        Log.w(LOG_TAG, "getLetterListenRecordsForStudent: fallback بعد فشل الاستعلام — " + r.message);
        OperationResult<ArrayList<LetterListenRecord>> all =
                getData(LetterListenRecord.COLLECTION_NAME, null, LetterListenRecord.class);
        if (!all.success || all.data == null) {
            return all;
        }
        ArrayList<LetterListenRecord> filtered = new ArrayList<>();
        for (LetterListenRecord rec : all.data) {
            if (studentId.equals(rec.getStudentRef())) {
                filtered.add(rec);
            }
        }
        return new OperationResult<>(true, "تصفية محلية", filtered);
    }

    /** مستندات تقدّم الطالب فقط. */
    public OperationResult<ArrayList<StudentProgress>> getStudentProgressForStudent(String studentId) {
        if (studentId == null || studentId.isEmpty()) {
            return new OperationResult<>(false, "معرف الطالب فارغ", new ArrayList<>());
        }
        List<String> queries = new ArrayList<>();
        queries.add(appwriteQueryEqual("studentId", studentId));
        queries.add(appwriteQueryLimit(50));
        OperationResult<ArrayList<StudentProgress>> r =
                getDataWithQueries("student_progress", null, StudentProgress.class, queries);
        if (r.success && r.data != null) {
            return r;
        }
        // نفس فكرة letter_listens: fallback بتصفية محلية بعد getData العام
        Log.w(LOG_TAG, "getStudentProgressForStudent: fallback بعد فشل الاستعلام — " + r.message);
        OperationResult<ArrayList<StudentProgress>> all =
                getData("student_progress", null, StudentProgress.class);
        if (!all.success || all.data == null) {
            return all;
        }
        ArrayList<StudentProgress> filtered = new ArrayList<>();
        for (StudentProgress p : all.data) {
            if (studentId.equals(p.getStudentId())) {
                filtered.add(p);
            }
        }
        return new OperationResult<>(true, "تصفية محلية", filtered);
    }
    
    /**
     * تحديث كائن موجود في قاعدة البيانات
     * @param data البيانات المحدثة
     * @param tableName اسم الجدول
     * @param documentId معرف المستند المراد تحديثه
     * @param collectionId معرف المجموعة (اختياري)
     * @return نتيجة العملية مع البيانات المحدثة
     * 
     * مثال على الاستخدام:
     * Product product = existingProduct; // منتج موجود
     * product.price = 799.99; // تحديث السعر
     * 
     * OperationResult<Product> result = dal.updateData(product, "products", 
     *                                                  "document-id-here", null);
     * 
     * if (result.success) {
     *     Log.d("SUCCESS", "تم تحديث المنتج بنجاح");
     *     Log.d("NEW_PRICE", "السعر الجديد: " + result.data.price);
     * } else {
     *     Log.e("ERROR", "فشل تحديث المنتج: " + result.message);
     * }
     */
    public <T> OperationResult<T> updateData(T data, String tableName, String documentId, String collectionId) {
        try {
            if (data == null || documentId == null || documentId.isEmpty()) {
                return new OperationResult<>(false, "البيانات أو معرف المستند لا يمكن أن يكون فارغاً");
            }
            
            Map<String, Object> documentData = convertObjectToMap(data);
            documentData.put("updatedAt", formatIsoUtc(new Date()));

            if (data instanceof StudentProgress) {
                encodeStudentProgressMapsForWrite(documentData, (StudentProgress) data);
                ensureStudentProgressStudentprogressId(documentData, documentId);
            }

            boolean updated = patchDocument(tableName, collectionId, documentId, documentData);
            
            if (updated) {
                return new OperationResult<>(true, "تم التحديث بنجاح", data);
            } else {
                return new OperationResult<>(false, "فشل في تحديث المستند");
            }
            
        } catch (Exception e) {
            return new OperationResult<>(false, "خطأ في تحديث البيانات: " + e.getMessage());
        }
    }
    
    /**
     * جلب عنصر واحد من قاعدة البيانات بواسطة معرفه
     * @param tableName اسم الجدول
     * @param documentId معرف المستند
     * @param collectionId معرف المجموعة (اختياري)
     * @param classType نوع الكلاس المطلوب
     * @return نتيجة العملية مع العنصر المحدد
     * 
     * مثال على الاستخدام:
     * OperationResult<Product> result = dal.getDataById("products", "document-id-here", null, Product.class);
     * 
     * if (result.success) {
     *     Product product = result.data;
     *     Log.d("PRODUCT", "الاسم: " + product.name + ", السعر: " + product.price);
     * } else {
     *     Log.e("ERROR", "فشل جلب المنتج: " + result.message);
     * }
     */
    public <T> OperationResult<T> getDataById(String tableName, String documentId, String collectionId, Class<T> classType) {
        try {
            if (documentId == null || documentId.isEmpty()) {
                return new OperationResult<>(false, "معرف المستند لا يمكن أن يكون فارغاً");
            }
            
            URL url = new URL(BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/" + 
                            (collectionId != null ? collectionId : tableName) + "/documents/" + documentId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JsonObject documentJson = gson.fromJson(response.toString(), JsonObject.class);
                if (documentJson == null) {
                    return new OperationResult<>(false, "فشل في تحويل البيانات");
                }
                flattenAppwriteDocumentData(documentJson);
                if (documentJson.has("$id")) {
                    documentJson.addProperty("id", documentJson.get("$id").getAsString());
                    documentJson.remove("$id");
                }

                T item = gson.fromJson(documentJson, classType);
                
                if (item != null) {
                    if (item instanceof StudentProgress) {
                        hydrateStudentProgressFromDocument((StudentProgress) item, documentJson);
                    }
                    try {
                        java.lang.reflect.Method setIdMethod = classType.getMethod("setId", String.class);
                        if (documentJson != null && documentJson.has("id")) {
                            setIdMethod.invoke(item, documentJson.get("id").getAsString());
                        }
                    } catch (Exception ignored) {
                    }
                    return new OperationResult<>(true, "تم جلب العنصر بنجاح", item);
                } else {
                    return new OperationResult<>(false, "فشل في تحويل البيانات");
                }
                
            } else {
                String errorMessage = readErrorResponse(connection);
                return new OperationResult<>(false, "فشل جلب العنصر: " + errorMessage);
            }
            
        } catch (Exception e) {
            return new OperationResult<>(false, "خطأ في جلب العنصر: " + e.getMessage());
        }
    }
    
    /**
     * إنشاء جدول جديد في قاعدة البيانات
     * @param tableName اسم الجدول
     * @param collectionId معرف المجموعة (اختياري)
     * @param schema هيكل الجدول (اختياري)
     * @return true إذا تم الإنشاء بنجاح، false إذا فشل
     * 
     * مثال على الاستخدام:
     * boolean created = dal.createTable("products", null, 
     *                                   "name:string,price:number,description:text");
     * 
     * if (created) {
     *     Log.d("SUCCESS", "تم إنشاء جدول المنتجات بنجاح");
     * } else {
     *     Log.e("ERROR", "فشل في إنشاء جدول المنتجات");
     * }
     */
    public boolean createTable(String tableName, String collectionId, String schema) {
        try {
            String actualCollectionId = collectionId != null ? collectionId : tableName;
            URL url = new URL(BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);
            
            // إعداد بيانات الجدول (Collection) بشكل صحيح لـ Appwrite
            Map<String, Object> collectionData = new HashMap<>();
            collectionData.put("collectionId", actualCollectionId);
            collectionData.put("name", tableName);
            collectionData.put("enabled", true); // تفعيل المجموعة
            
            String jsonBody = gson.toJson(collectionData);
            
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes());
            }
            
            int responseCode = connection.getResponseCode();
            String response = "";
            if (responseCode >= 200 && responseCode < 300) {
                // قراءة الاستجابة الناجحة
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                reader.close();
                response = responseBuilder.toString();
                
                // تفعيل إنشاء schema attributes
                if (schema != null && !schema.isEmpty()) {
                    createTableAttributes(actualCollectionId, schema, tableName);
                    
                    // انتظار 3 ثواني لتفعيل attributes في Appwrite
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                    }
                } else {
                    // إضافة schema افتراضي للملابس
                    if (tableName.toLowerCase().contains("clothes")) {
                        createDefaultClothesSchema(actualCollectionId, tableName);
                    }
                }
                
                return true;
            } else {
                // قراءة رسالة الخطأ
                String errorResponse = readErrorResponse(connection);
                return false;
            }
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * فحص وجود جدول في قاعدة البيانات
     * @param tableName اسم الجدول
     * @param collectionId معرف المجموعة (اختياري)
     * @return true إذا كان الجدول موجوداً، false إذا لم يكن كذلك
     */
    public boolean tableExists(String tableName, String collectionId) {
        try {
            // محاولة الوصول إلى جداول قاعدة البيانات
            URL url = new URL(BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200) {
                // قراءة قائمة الجداول الموجودة
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                String responseString = response.toString();
                String expectedCollectionId = collectionId != null ? collectionId : tableName;
                
                // البحث عن الجدول في الاستجابة
                
                // إصلاح البحث الدقيق - البحث عن المطابقة التامة في $id أو name
                // البحث عن "$id\":\"expectedCollectionId\" أو "\"name\":\"expectedCollectionId\""
                boolean found = false;
                String searchPatternId = "\"$id\":\"" + expectedCollectionId + "\"";
                String searchPatternName = "\"name\":\"" + expectedCollectionId + "\"";
                
                if (responseString.contains(searchPatternId) || responseString.contains(searchPatternName)) {
                    found = true;
                } else {
                    // في حالة فشل المطابقة الدقيقة، نبحث عن تطابق جزئي آمن
                    // نتحقق من أن الاسم الصحيح موجود في قائمة collections
                    String collectionsPattern = "\"collections\":[";
                    int collectionsIndex = responseString.indexOf(collectionsPattern);
                    
                    if (collectionsIndex != -1) {
                        String collectionsContent = responseString.substring(collectionsIndex);
                        
                        // استخراج جميع IDs والأسماء
                        if (collectionsContent.contains("\"" + expectedCollectionId + "\"")) {
                            found = true;
                        } else {
                            // محاولة أخيرة: البحث عن جداول مماثلة للملابس
                            // إذا كان البحث عن "clothes" وجدنا "clothes2"، نعتبره تطابق
                            if (expectedCollectionId.equals("clothes") && collectionsContent.contains("clothes2")) {
                                found = true;
                            } else {
                                found = false;
                            }
                        }
                    } else {
                        found = false;
                    }
                }
                
                return found;
            } else {
                return false;
            }
            
        } catch (Exception e) {
            Log.w(LOG_TAG, "tableExists خطأ لـ \"" + tableName + "\" — سنعتبر المجموعة غير موجودة لمحاولة الإنشاء", e);
            return false;
        }
    }

    /** تواريخ Appwrite كسلسلة ISO UTC */
    private static String formatIsoUtc(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    private static Object normalizeAppwriteValue(Object value) {
        if (value instanceof Date) {
            return formatIsoUtc((Date) value);
        }
        return value;
    }
    
    // === دوال مساعدة لقواعد البيانات ===
    
    /**
     * التأكد من وجود الجدول وإنشاؤه إذا لم يكن موجوداً
     * @param tableName اسم الجدول
     * @param collectionId معرف المجموعة (اختياري)
     * @param schema تعريف schema للجدول (اختياري)
     * @return true إذا كان موجوداً أو تم إنشاؤه بنجاح
     */
    private boolean ensureTableExists(String tableName, String collectionId, String schema) {
        String expectedName = collectionId != null ? collectionId : tableName;
        
        // أولاً، تأكد من وجود الجدول
        if (tableExists(tableName, collectionId)) {
            return true;
        } else {
            return createTable(tableName, collectionId, schema);
        }
    }
    
    /**
     * التأكد من وجود الجدول وإنشاؤه إذا لم يكن موجوداً (signature قديم للتوافق)
     * @param tableName اسم الجدول
     * @param collectionId معرف المجموعة (اختياري)
     * @return true إذا كان موجوداً أو تم إنشاؤه بنجاح
     */
    private boolean ensureTableExists(String tableName, String collectionId) {
        return ensureTableExists(tableName, collectionId, null);
    }
    
    /**
     * حفظ مستند في قاعدة البيانات
     * @param tableName اسم الجدول
     * @param collectionId معرف المجموعة (اختياري)
     * @param documentId معرف المستند
     * @param documentData بيانات المستند
     * @return true إذا تم الحفظ بنجاح، false إذا فشل
     */
    private boolean saveDocument(String tableName, String collectionId, String documentId, Map<String, Object> documentData) {
        try {
            URL url = new URL(BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/" + 
                            (collectionId != null ? collectionId : tableName) + "/documents");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);
            
            // إنشاء طلب JSON بسيط يتجاهل schema attributes
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("documentId", documentId); // ID المستند
            
            // حفظ البيانات مباشرة بدون schema validation
            // تنظيف البيانات من أي حقول غير معرفة في schema
            Map<String, Object> cleanData = new HashMap<>();
            
            // نسخ البيانات الأساسية فقط (name, price, dateAdded, imageUrl, id)
            for (Map.Entry<String, Object> entry : documentData.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                // تجاهل الحقول غير المطلوبة
                if (key.equals("metadata") || key.equals("documentId") || 
                    key.equals("createdAt") || key.equals("createdBy") ||
                    key.equals("class") || key.equals("$") || key.equals("id")) {
                    continue; // لا نضيف هذه الحقول (id تم تحويله مسبقاً)
                }
                
                // إضافة الحقول المهمة فقط
                cleanData.put(key, normalizeAppwriteValue(value));
            }
            
            requestBody.put("data", cleanData);
            
            String jsonBody = gson.toJson(requestBody);

            Log.d(LOG_TAG, "POST document collection=" + (collectionId != null ? collectionId : tableName)
                    + " documentId=" + documentId);
            
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode >= 200 && responseCode < 300) {
                Log.d(LOG_TAG, "POST document نجح code=" + responseCode);
                return true;
            } else {
                String errorResponse = readErrorResponse(connection);
                Log.e(LOG_TAG, "POST document فشل code=" + responseCode + " collection="
                        + (collectionId != null ? collectionId : tableName) + " → " + errorResponse);
                
                if (responseCode == 409 && errorResponse.contains("document_already_exists")) {
                    Log.d(LOG_TAG, "POST 409 — المستند موجود، استخدام PATCH");
                    return patchDocument(tableName, collectionId, documentId, documentData);
                }
                
                // إصلاح schema errors: محاولة أخرى بدون schema validation
                if (errorResponse.contains("Unknown attribute") || errorResponse.contains("document_invalid_structure")) {
                    return saveWithoutSchemaValidation(tableName, collectionId, documentId, cleanData);
                }
                
                return false;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "saveDocument استثناء collection=" + tableName, e);
            return false;
        }
    }

    /**
     * تحديث حقول مستند موجود (REST PATCH) — مناسب لتحديثات جزئية.
     */
    private boolean patchDocument(String tableName, String collectionId, String documentId,
                                  Map<String, Object> documentData) {
        try {
            URL url = new URL(BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/" +
                    (collectionId != null ? collectionId : tableName) + "/documents/" + documentId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PATCH");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);

            Map<String, Object> cleanData = new HashMap<>();
            for (Map.Entry<String, Object> entry : documentData.entrySet()) {
                String key = entry.getKey();
                if (key.equals("metadata") || key.equals("documentId") ||
                        key.equals("createdAt") || key.equals("createdBy") ||
                        key.equals("class") || key.equals("$") || key.equals("id")) {
                    continue;
                }
                cleanData.put(key, normalizeAppwriteValue(entry.getValue()));
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("data", cleanData);
            String jsonBody = gson.toJson(requestBody);

            Log.d(LOG_TAG, "PATCH document collection=" + (collectionId != null ? collectionId : tableName)
                    + " documentId=" + documentId);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                Log.d(LOG_TAG, "PATCH نجح code=" + responseCode);
                return true;
            }
            String err = readErrorResponse(connection);
            Log.e(LOG_TAG, "PATCH فشل code=" + responseCode + " → " + err);
            return false;
        } catch (Exception e) {
            Log.e(LOG_TAG, "patchDocument استثناء", e);
            return false;
        }
    }
    
    /**
     * حفظ البيانات بدون schema validation نهائياً
     * 
     * ملاحظة: هذا حل مؤقت للمشكلة:
     * - Appwrite schema attributes API endpoints تعطي خطأ 404 في v1.8.0
     * - الجداول الموجودة فارغة من schema attributes
     * - الحل الأمثل: إنشاء schema attributes يدوياً من Appwrite dashboard
     * 
     * Schema attributes المطلوبة لـ clothes2:
     * - name: string (required)
     * - price: double (required) 
     * - dateAdded: datetime (required)
     * - imageUrl: string (optional)
     */
    private boolean saveWithoutSchemaValidation(String tableName, String collectionId, String documentId, Map<String, Object> documentData) {
        try {
            URL url = new URL(BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/" + 
                            (collectionId != null ? collectionId : tableName) + "/documents");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);
            
            // تنظيف إضافي: إزالة أي حقول غير مطلوبة
            Map<String, Object> cleanData = new HashMap<>(documentData);
            cleanData.remove("metadata");
            cleanData.remove("documentId");
            cleanData.remove("createdAt");
            cleanData.remove("createdBy");
            
            // طلب JSON مبسط جداً - يجب تضمين documentId
            Map<String, Object> simpleRequest = new HashMap<>();
            simpleRequest.put("documentId", documentId); // إضافة documentId
            simpleRequest.put("data", cleanData);
            
            String jsonBody = gson.toJson(simpleRequest);
            
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes());
            }
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode >= 200 && responseCode < 300) {
                return true;
            } else {
                String errorResponse = readErrorResponse(connection);
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * تحويل كائن Java إلى Map
     * @param obj الكائن المراد تحويله
     * @return Map يحتوي على بيانات الكائن
     */
    private Map<String, Object> convertObjectToMap(Object obj) {
        try {
            String json = gson.toJson(obj);
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            return gson.fromJson(json, type);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
    
    /**
     * الحصول على معرف الكائن (ID)
     * @param obj الكائن المراد فحصه
     * @return معرف الكائن أو null إذا لم يكن موجوداً
     */
    private String getObjectId(Object obj) {
        try {
            Map<String, Object> map = convertObjectToMap(obj);
            Object id = map.get("id");
            if (id == null) {
                id = map.get("userId");
            }
            if (id == null) {
                id = map.get("documentId");
            }
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * تحليل استجابة جلب المستندات وتحويلها لكائنات Java
     * @param responseString استجابة JSON
     * @param classType نوع الكلاس المطلوب
     * @return قائمة الكائنات
     */
    private <T> ArrayList<T> parseDocumentResponse(String responseString, Class<T> classType) {
        try {
            ArrayList<T> items = new ArrayList<>();
            
            // استخدام Gson لتحليل الاستجابة بشكل صحيح
            JsonObject jsonResponse = gson.fromJson(responseString, JsonObject.class);
            
            if (jsonResponse != null && jsonResponse.has("documents")) {
                JsonArray documentsArray = jsonResponse.getAsJsonArray("documents");
                
                for (int i = 0; i < documentsArray.size(); i++) {
                    try {
                        JsonObject documentJson = documentsArray.get(i).getAsJsonObject();

                        flattenAppwriteDocumentData(documentJson);
                        
                        // تحويل $id من Appwrite إلى id للكائن Java
                        if (documentJson.has("$id")) {
                            String documentId = documentJson.get("$id").getAsString();
                            documentJson.addProperty("id", documentId);
                            documentJson.remove("$id");
                        }
                        
                        // تحويل المستند إلى كائن Java
                        T item = gson.fromJson(documentJson, classType);
                        
                        if (item != null) {
                            if (item instanceof StudentProgress) {
                                hydrateStudentProgressFromDocument((StudentProgress) item, documentJson);
                            }
                            // التأكد من تعيين ID في الكائن
                            try {
                                java.lang.reflect.Method setIdMethod = classType.getMethod("setId", String.class);
                                if (documentJson.has("id")) {
                                    String id = documentJson.get("id").getAsString();
                                    setIdMethod.invoke(item, id);
                                }
                            } catch (Exception e) {
                                // لا توجد setter للـ id، تجاهل
                            }
                            
                            items.add(item);
                        }
                    } catch (Exception e) {
                        // خطأ في تحليل مستند
                    }
                }
            }
            
            return items;
        } catch (Exception e) {
            // خطأ في تحليل الاستجابة
            return new ArrayList<>();
        }
    }

    /**
     * يدمج كائن {@code data} الذي تعيده Appwrite داخل المستند حتى يقرأ Gson الحقول مباشرة.
     */
    private void flattenAppwriteDocumentData(JsonObject documentJson) {
        if (documentJson == null || !documentJson.has("data") || !documentJson.get("data").isJsonObject()) {
            return;
        }
        JsonObject data = documentJson.getAsJsonObject("data");
        for (Map.Entry<String, JsonElement> e : data.entrySet()) {
            documentJson.add(e.getKey(), e.getValue());
        }
    }

    /**
     * مخطط Appwrite غالبًا يعرّف lettersLearned / wordsProgress كـ string — نخزّن JSON كنص.
     */
    private void encodeStudentProgressMapsForWrite(Map<String, Object> documentData, StudentProgress sp) {
        documentData.put("lettersLearned", gson.toJson(sp.getLettersLearned()));
        Map<String, Integer> wp = sp.getWordsProgress();
        documentData.put("wordsProgress", gson.toJson(wp != null ? wp : new HashMap<String, Integer>()));
        documentData.put("lettersQuizPassed", gson.toJson(sp.getLettersQuizPassed()));
        documentData.put("letterQuizHistory", gson.toJson(sp.getLetterQuizHistory()));
    }

    /**
     * يطابق مخطط Appwrite الذي يفرض السمة {@code studentprogressId} (نفس منطق إعادة تسمية {@code id} في saveData).
     */
    private static void ensureStudentProgressStudentprogressId(Map<String, Object> documentData, String documentId) {
        if (documentId == null || documentId.isEmpty()) {
            return;
        }
        Object existing = documentData.get("studentprogressId");
        if (existing == null || existing.toString().isEmpty()) {
            documentData.put("studentprogressId", documentId);
        }
    }

    private void hydrateStudentProgressFromDocument(StudentProgress sp, JsonObject documentJson) {
        Type boolMapType = new TypeToken<Map<String, Boolean>>(){}.getType();
        Type intMapType = new TypeToken<Map<String, Integer>>(){}.getType();

        if (documentJson.has("lettersLearned")) {
            JsonElement el = documentJson.get("lettersLearned");
            try {
                Map<String, Boolean> m = parseJsonMap(el, boolMapType);
                if (m != null) {
                    sp.setLettersLearned(m);
                }
            } catch (Exception e) {
                Log.w(LOG_TAG, "hydrateStudentProgress lettersLearned", e);
            }
        }
        if (documentJson.has("wordsProgress")) {
            JsonElement el = documentJson.get("wordsProgress");
            try {
                Map<String, Integer> m = parseJsonMap(el, intMapType);
                if (m != null) {
                    sp.setWordsProgress(m);
                }
            } catch (Exception e) {
                Log.w(LOG_TAG, "hydrateStudentProgress wordsProgress", e);
            }
        }
        if (documentJson.has("lettersQuizPassed")) {
            JsonElement el = documentJson.get("lettersQuizPassed");
            try {
                Map<String, Boolean> m = parseJsonMap(el, boolMapType);
                if (m != null) {
                    sp.setLettersQuizPassed(m);
                }
            } catch (Exception e) {
                Log.w(LOG_TAG, "hydrateStudentProgress lettersQuizPassed", e);
            }
        }
        if (documentJson.has("letterQuizHistory")) {
            JsonElement el = documentJson.get("letterQuizHistory");
            Type listSessionType = new TypeToken<ArrayList<LetterQuizSession>>(){}.getType();
            try {
                ArrayList<LetterQuizSession> sessions = null;
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                    sessions = gson.fromJson(el.getAsString(), listSessionType);
                } else if (el.isJsonArray()) {
                    sessions = gson.fromJson(el, listSessionType);
                }
                if (sessions != null) {
                    sp.setLetterQuizHistory(sessions);
                }
            } catch (Exception e) {
                Log.w(LOG_TAG, "hydrateStudentProgress letterQuizHistory", e);
            }
        }
        sp.normalizeLetterMapKeys();
    }

    private <M> M parseJsonMap(JsonElement el, Type mapType) {
        if (el == null || el.isJsonNull()) {
            return null;
        }
        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
            return gson.fromJson(el.getAsString(), mapType);
        }
        if (el.isJsonObject()) {
            return gson.fromJson(el, mapType);
        }
        return null;
    }
    
    // === دوال إدارة التخزين والملفات ===
    
    /**
     * رفع ملف إلى التخزين وحفظ معلوماته في جدول خاص
     * @param fileData بيانات الملف (byte array)
     * @param fileName اسم الملف
     * @param mimeType نوع الملف (مثل "image/jpeg", "application/pdf")
     * @param bucketId معرف التخزين (اختياري، يستخدم التخزين الرئيسي إذا null)
     * @return نتيجة العملية مع معلومات الملف المرفوع
     * 
     * مثال على الاستخدام:
     * // رفع صورة
     * byte[] imageData = getImageBytesFromCamera(); // الحصول على بيانات الصورة
     * 
     * OperationResult<FileInfo> result = dal.uploadFile(imageData, "photo.jpg", "image/jpeg", null);
     * 
     * if (result.success) {
     *     FileInfo fileInfo = result.data;
     *     Log.d("SUCCESS", "تم رفع الملف بنجاح");
     *     Log.d("FILE_URL", "رابط الملف: " + fileInfo.fileUrl);
     *     Log.d("FILE_ID", "معرف الملف: " + fileInfo.fileId);
     * } else {
     *     Log.e("ERROR", "فشل رفع الملف: " + result.message);
     * }
     * 
     * // رفع مستند PDF
     * byte[] pdfData = getPdfBytes();
     * OperationResult<FileInfo> pdfResult = dal.uploadFile(pdfData, "document.pdf", "application/pdf", null);
     */
    public OperationResult<FileInfo> uploadFile(byte[] fileData, String fileName, String mimeType, String bucketId) {
        try {
            // التحقق من صحة البيانات
            if (fileData == null || fileData.length == 0) {
                return new OperationResult<>(false, "بيانات الملف لا يمكن أن تكون فارغة");
            }
            
            if (fileName == null || fileName.isEmpty()) {
                return new OperationResult<>(false, "اسم الملف لا يمكن أن يكون فارغاً");
            }
            
            // استخدام التخزين الرئيسي إذا لم يتم تحديد تخزين آخر
            String actualBucketId = bucketId != null ? bucketId : MAIN_STORAGE_BUCKET_ID;
            
            // إنشاء معرف فريد للملف
            String fileId = UUID.randomUUID().toString();
            
            // إعداد طلب رفع الملف
            URL url = new URL(BASE_URL + "/storage/buckets/" + actualBucketId + "/files");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=----WebKitFormBoundary");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(30000); // 30 ثانية للملفات الكبيرة
            connection.setReadTimeout(30000);
            connection.setDoOutput(true);
            
            // إنشاء multipart form data
            String boundary = "----WebKitFormBoundary";
            
            StringBuilder formData = new StringBuilder();
            formData.append("--").append(boundary).append("\r\n");
            formData.append("Content-Disposition: form-data; name=\"fileId\"\r\n\r\n");
            formData.append(fileId).append("\r\n");
            
            formData.append("--").append(boundary).append("\r\n");
            formData.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"\r\n");
            formData.append("Content-Type: ").append(mimeType != null ? mimeType : "application/octet-stream").append("\r\n\r\n");
            
            byte[] headerBytes = formData.toString().getBytes("utf-8");
            byte[] footerBytes = ("\r\n--" + boundary + "--\r\n").getBytes("utf-8");
            
            // كتابة البيانات
            try (OutputStream os = connection.getOutputStream()) {
                os.write(headerBytes);
                os.write(fileData);
                os.write(footerBytes);
            }
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 201 || responseCode == 200) {
                // قراءة الاستجابة
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                // تحليل الاستجابة وإنشاء معلومات الملف
                FileInfo fileInfo = parseFileResponse(response.toString(), fileName, mimeType, actualBucketId, fileId);
                
                return new OperationResult<>(true, "تم رفع الملف بنجاح", fileInfo);
                
            } else {
                String errorMessage = readErrorResponse(connection);
                return new OperationResult<>(false, "فشل رفع الملف: " + errorMessage);
            }
            
        } catch (Exception e) {
            return new OperationResult<>(false, "خطأ في رفع الملف: " + e.getMessage());
        }
    }

    /**
     * تنزيل ملف من رابط تحميل Appwrite Storage مع مصادقة الخادم.
     * طلبات Glide العادية بدون الرؤوس تعيد 401.
     */
    public OperationResult<byte[]> downloadStorageAuthenticated(String fileDownloadUrl) {
        if (fileDownloadUrl == null || fileDownloadUrl.isEmpty()) {
            return new OperationResult<>(false, "رابط فارغ", null);
        }
        HttpURLConnection connection = null;
        try {
            URL url = new URL(fileDownloadUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                try (InputStream in = connection.getInputStream();
                     ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
                    byte[] chunk = new byte[8192];
                    int n;
                    while ((n = in.read(chunk)) != -1) {
                        buf.write(chunk, 0, n);
                    }
                    byte[] bytes = buf.toByteArray();
                    return new OperationResult<>(true, "تم التنزيل", bytes);
                }
            }
            String err = readErrorResponse(connection);
            return new OperationResult<>(false, "HTTP " + responseCode + ": " + err, null);
        } catch (Exception e) {
            Log.e(LOG_TAG, "downloadStorageAuthenticated", e);
            return new OperationResult<>(false, e.getMessage(), null);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    // === دوال مساعدة للتخزين ===
    
    /**
     * تحليل استجابة رفع الملف
     * @param responseString استجابة JSON
     * @param fileName اسم الملف
     * @param mimeType نوع الملف
     * @param bucketId معرف التخزين
     * @param fileId معرف الملف
     * @return معلومات الملف
     */
    private FileInfo parseFileResponse(String responseString, String fileName, String mimeType, String bucketId, String fileId) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.fileId = fileId;
        fileInfo.fileName = fileName;
        fileInfo.mimeType = mimeType;
        fileInfo.bucketId = bucketId;
        fileInfo.uploadedBy = "system";
        fileInfo.uploadDate = new Date();
        
        try {
            // بناء الرابط الصحيح للملف في Appwrite Frankfurt region
            // استخدام /download مع API key في الرابط للوصول للملف
            fileInfo.fileUrl = BASE_URL + "/storage/buckets/" + bucketId + "/files/" + fileId + "/download?project=" + PROJECT_ID;
            
            // محاولة استخراج حجم الملف إذا كان متوفراً من الاستجابة
            String sizeString = extractValue(responseString, "sizeOriginal");
            if (sizeString == null || sizeString.isEmpty()) {
                sizeString = extractValue(responseString, "size");
            }
            
            if (sizeString != null && !sizeString.isEmpty()) {
                try {
                    fileInfo.fileSize = Long.parseLong(sizeString);
                } catch (NumberFormatException e) {
                    fileInfo.fileSize = 0;
                }
            }
            
        } catch (Exception e) {
            // في حالة الخطأ، استخدم الرابط الافتراضي
            fileInfo.fileUrl = BASE_URL + "/storage/buckets/" + bucketId + "/files/" + fileId + "/download?project=" + PROJECT_ID;
        }
        
        return fileInfo;
    }
    
    /**
     * إنشاء schema افتراضي لجدول الملابس
     * @param collectionId معرف المجموعة
     * @param tableName اسم الجدول
     */
    private void createDefaultClothesSchema(String collectionId, String tableName) {
        try {
            
            // تحديد attributes للملابس
            String[] attributes = {
                "name:string:text:نص",
                "price:number:decimal:السعر",
                "dateAdded:date:datetime:تاريخ الإضافة",
                "imageUrl:string:url:رابط الصورة"
            };
            
            for (String attribute : attributes) {
                createAttribute(collectionId, attribute, tableName);
            }
            
        } catch (Exception e) {
        }
    }
    
    /**
     * إضافة attribute واحد للجدول
     * @param collectionId معرف المجموعة
     * @param attribute تعريف الـ attribute بصيغة "name:type:required:label"
     * @param tableName اسم الجدول (للمساعدة في logging)
     */
    private void createAttribute(String collectionId, String attribute, String tableName) {
        try {
            String[] parts = attribute.split(":");
            if (parts.length < 2) {
                return;
            }
            
            String name = parts[0];
            String type = parts[1];
            boolean required = parts.length > 2 ? Boolean.parseBoolean(parts[2]) : false;
            String label = parts.length > 3 ? parts[3] : name;
            
            // تحويل أنواع البيانات لـ Appwrite
            String appwriteType;
            int size = 255; // الحجم الافتراضي للنصوص
            
            switch (type.toLowerCase()) {
                case "string":
                case "text":
                case "url":
                    appwriteType = "string";
                    size = type.equalsIgnoreCase("url") ? 500 : 255;
                    break;
                case "number":
                case "decimal":
                case "double":
                case "float":
                    appwriteType = "float"; // Appwrite uses "float" not "double"
                    break;
                case "integer":
                case "int":
                    appwriteType = "integer";
                    break;
                case "date":
                case "datetime":
                    appwriteType = "datetime";
                    break;
                case "boolean":
                    appwriteType = "boolean";
                    break;
                default:
                    appwriteType = "string";
            }
            
            // إصلاح API endpoint للـ attributes في Appwrite v1.8.0
            // الـ endpoint الصحيح: POST /databases/{databaseId}/collections/{collectionId}/attributes/{type}
            String endpoint;
            if (appwriteType.equals("string")) {
                endpoint = BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/" + 
                          collectionId + "/attributes/string";
            } else if (appwriteType.equals("float")) {
                endpoint = BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/" + 
                          collectionId + "/attributes/float";
            } else if (appwriteType.equals("integer")) {
                endpoint = BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/" + 
                          collectionId + "/attributes/integer";
            } else if (appwriteType.equals("datetime")) {
                endpoint = BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/" + 
                          collectionId + "/attributes/datetime";
            } else if (appwriteType.equals("boolean")) {
                endpoint = BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/" + 
                          collectionId + "/attributes/boolean";
            } else {
                endpoint = BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections/" + 
                          collectionId + "/attributes/string";
            }
            
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);
            
            Map<String, Object> attributeData = new HashMap<>();
            attributeData.put("key", name); // اسم الحقل
            attributeData.put("required", required);
            
            // إضافة خصائص خاصة بكل نوع
            if (appwriteType.equals("string")) {
                attributeData.put("size", size);
                attributeData.put("default", null);
            } else if (appwriteType.equals("float")) {
                attributeData.put("min", null);
                attributeData.put("max", null);
                attributeData.put("default", null);
            } else if (appwriteType.equals("integer")) {
                attributeData.put("min", null);
                attributeData.put("max", null);
                attributeData.put("default", null);
            } else if (appwriteType.equals("datetime")) {
                attributeData.put("default", null);
            }
            
            String jsonBody = gson.toJson(attributeData);
            
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes());
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                // تم إضافة الصلاحية بنجاح
            } else {
                String errorResponse = readErrorResponse(connection);
                // فشل إضافة الصلاحية
            }
            
        } catch (Exception e) {
            // خطأ في تحليل الاستجابة
        }
    }
    
    /**
     * إضافة table attributes مخصصة
     * @param collectionId معرف المجموعة
     * @param schema تعريف schema بصيغة "name:type:required:label,name2:type2:required2:label2"
     * @param tableName اسم الجدول
     */
    private void createTableAttributes(String collectionId, String schema, String tableName) {
        try {
            String[] attributes = schema.split(",");
            for (String attribute : attributes) {
                createAttribute(collectionId, attribute.trim(), tableName);
            }
        } catch (Exception e) {
        }
    }
    
    /**
     * العثور على الـ collection ID الفعلي لجدول محدد
     * @param tableName اسم الجدول المطلوب
     * @param collectionId معرف المجموعة (اختياري)
     * @return الـ collection ID الفعلي، أو null إذا لم يتم العثور عليه
     */
    private String getActualCollectionId(String tableName, String collectionId) {
        try {
            String expectedCollectionId = collectionId != null ? collectionId : tableName;
            
            // محاولة الوصول إلى جداول قاعدة البيانات
            URL url = new URL(BASE_URL + "/databases/" + MAIN_DATABASE_ID + "/collections");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Appwrite-Project", PROJECT_ID);
            connection.setRequestProperty("X-Appwrite-Key", API_KEY);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == 200) {
                // قراءة قائمة الجداول الموجودة
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                String responseString = response.toString();
                
                // البحث الدقيق عن الجدول
                String searchPatternId = "\"$id\":\"" + expectedCollectionId + "\"";
                String searchPatternName = "\"name\":\"" + expectedCollectionId + "\"";
                
                if (responseString.contains(searchPatternId) || responseString.contains(searchPatternName)) {
                    return expectedCollectionId;
                }
                
                // البحث عن جدول موجود يحتوي على الاسم المطلوب
                String collectionsPattern = "\"collections\":[";
                int collectionsIndex = responseString.indexOf(collectionsPattern);
                
                if (collectionsIndex != -1) {
                    String collectionsContent = responseString.substring(collectionsIndex);
                    
                    // إذا وجد تطابق جزئي
                    if (collectionsContent.contains("\"" + expectedCollectionId + "\"")) {
                        
                        // استخراج الـ $id الفعلي من الاستجابة
                        String actualId = extractCollectionIdFromResponse(responseString, expectedCollectionId);
                        if (actualId != null) {
                            return actualId;
                        } else {
                            return expectedCollectionId;
                        }
                    } else {
                        // محاولة أخيرة: البحث عن جداول مماثلة للملابس
                        if (expectedCollectionId.equals("clothes") && collectionsContent.contains("clothes2")) {
                            
                            // استخراج clothes2 كـ actual ID
                            String clothes2Id = extractCollectionIdFromResponse(responseString, "clothes2");
                            if (clothes2Id != null) {
                                return clothes2Id;
                            } else {
                                return "clothes2";
                            }
                        }
                    }
                }
                
                return null;
            } else {
                return null;
            }
            
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * استنتاج schema من كائن تلقائياً
     * @param obj الكائن المراد استنتاج schema منه
     * @return schema بصيغة "key:type:required:label,..."
     */
    private String inferSchemaFromObject(Object obj) {
        try {
            Map<String, Object> map = convertObjectToMap(obj);
            StringBuilder schema = new StringBuilder();
            
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                // تجاهل الحقول الخاصة
                if (key.equals("class") || key.equals("$") || key.equals("metadata") ||
                    key.equals("documentId") || key.equals("createdAt") || key.equals("createdBy")) {
                    continue;
                }
                
                // تحويل "id" إلى اسم فريد
                if (key.equals("id")) {
                    String className = obj.getClass().getSimpleName().toLowerCase();
                    key = className + "Id";
                }
                
                // تحديد نوع البيانات
                String type = "string"; // افتراضي
                boolean required = false; // افتراضي
                
                if (value != null) {
                    if (value instanceof String) {
                        String strValue = (String) value;
                        if (strValue.isEmpty()) {
                            required = false; // فارغ = اختياري
                        } else {
                            type = "string";
                            required = true;
                        }
                    } else if (value instanceof Integer) {
                        type = "integer";
                        required = true;
                    } else if (value instanceof Long) {
                        type = "integer";
                        required = true;
                    } else if (value instanceof Float || value instanceof Double) {
                        type = "float";
                        required = true;
                    } else if (value instanceof Boolean) {
                        type = "boolean";
                        required = true;
                    } else if (value instanceof java.util.Date) {
                        type = "datetime";
                        required = true;
                    } else {
                        type = "string";
                        required = false;
                    }
                } else {
                    // null = اختياري
                    type = "string";
                    required = false;
                }
                
                // إضافة للschema
                if (schema.length() > 0) {
                    schema.append(",");
                }
                schema.append(key).append(":").append(type).append(":").append(required).append(":").append(key);
            }
            
            return schema.toString();
            
        } catch (Exception e) {
            return null;
        }
    }
    
    private String extractCollectionIdFromResponse(String responseString, String expectedCollectionId) {
        try {
            // البحث عن \"name\":\"expectedCollectionId\" في الاستجابة
            String namePattern = "\"name\":\"" + expectedCollectionId + "\"";
            int nameStart = responseString.indexOf(namePattern);
            
            if (nameStart != -1) {
                // البحث عن بداية object التالي
                int objectStart = responseString.indexOf("{", nameStart);
                if (objectStart != -1) {
                    // البحث عن \"$id\":\"actualId\" في نفس الـ object
                    String idPattern = "\"$id\":\"";
                    int idStart = responseString.indexOf(idPattern, objectStart);
                    
                    if (idStart != -1) {
                        int idContentStart = idStart + idPattern.length();
                        int idEnd = responseString.indexOf("\"", idContentStart);
                        
                        if (idEnd != -1) {
                            return responseString.substring(idContentStart, idEnd);
                        }
                    }
                }
            }
            
            // طريقة بديلة: البحث عن $id مباشرة
            String directIdPattern = "\"name\":\"" + expectedCollectionId + "\",\"$id\":\"";
            int directStart = responseString.indexOf(directIdPattern);
            
            if (directStart != -1) {
                int idStart = directStart + directIdPattern.length();
                int idEnd = responseString.indexOf("\"", idStart);
                
                if (idEnd != -1) {
                    return responseString.substring(idStart, idEnd);
                }
            }
            
        } catch (Exception e) {
        }
        return null;
    }
}
