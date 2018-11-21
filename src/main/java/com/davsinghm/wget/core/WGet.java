package com.davsinghm.wget.core;

public class WGet {

//    public static final String UTF8 = "UTF-8";
//
//    private DownloadInfo info;
//
//    Direct d;
//
//    File targetFile;
//
//    public interface HtmlLoader {
//        /**
//         * some socket problem, retyring
//         *
//         * @param retry
//         * @param delay
//         * @param e
//         */
//        public void notifyRetry(int retry, int delay, Throwable e);
//
//        /**
//         * start downloading
//         */
//        public void notifyDownloading();
//
//        /**
//         * document moved, relocating
//         */
//        public void notifyMoved();
//    }
//
//    /**
//     * download with events control.
//     *
//     * @param source
//     * @param target
//     */
//    public WGet(URL source, File target) {
//        create(source, target);
//    }
//
//    /**
//     * application controlled download / resume. you should specify targetfile name exactly. since you are choice resume
//     * / multipart download. application unable to control file name choice / creation.
//     *
//     * @param info       download info
//     * @param targetFile target files
//     */
//    public WGet(DownloadInfo info, File targetFile) {
//        this.info = info;
//        this.targetFile = targetFile;
//        create();
//    }
//
//    void create(URL source, File target) {
//        info = new DownloadInfo(source);
//        info.extract();
//        create(target);
//    }
//
//    void create(File target) {
//        targetFile = calcName(info, target);
//        create();
//    }
//
//    void create() {
//        d = createDirect();
//    }
//
//    Direct createDirect() {
//        if (info.multipart()) {
//            return new DirectMultipart(info, targetFile);
//        } else if (info.getRange()) {
//            return new DirectRange(info, targetFile);
//        } else {
//            return new DirectSingle(info, targetFile);
//        }
//    }
//
//    public static File calcName(URL source, File target) {
//        DownloadInfo info = new DownloadInfo(source);
//        info.extract();
//        return calcName(info, target);
//    }
//
//    /**
//     * Generate file name
//     *
//     * @param info   download info
//     * @param target 1) can point to directory - generate exclusive (1) name. 2) to existing file 3) to non existing file
//     * @return
//     */
//    public static File calcName(DownloadInfo info, File target) {
//        String name = null;
//
//        name = info.getContentFilename();
//
//        if (name == null)
//            name = new File(info.getSource().getPath()).getName();
//
//        try {
//            name = URLDecoder.decode(name, UTF8);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//        String nameNoExt = FilenameUtils.removeExtension(name);
//        String ext = FilenameUtils.getExtension(name);
//
//        File targetFile = null;
//
//        if (target.isDirectory()) {
//            targetFile = FileUtils.getFile(target, name);
//            int i = 1;
//            while (targetFile.exists()) {
//                targetFile = FileUtils.getFile(target, nameNoExt + " (" + i + ")." + ext);
//                i++;
//            }
//        } else {
//            try {
//                FileUtils.forceMkdir(new File(target.getParent()));
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//            targetFile = target;
//        }
//
//        return targetFile;
//    }
//
//    public void download() {
//        download(new AtomicBoolean(false), new Runnable() {
//            @Override
//            public void run() {
//            }
//        });
//    }
//
//    public void download(AtomicBoolean stop, Runnable notify) {
//        d.download(stop, notify);
//    }
//
//    public DownloadInfo getInfo() {
//        return info;
//    }
//
//    public static String getHtml(URL source) {
//        return getHtml(source, new HtmlLoader() {
//            @Override
//            public void notifyRetry(int retry, int delay, Throwable e) {
//            }
//
//            @Override
//            public void notifyDownloading() {
//            }
//
//            @Override
//            public void notifyMoved() {
//            }
//        }, new AtomicBoolean(false));
//    }
//
//    public static String getHtml(DownloadInfo info) {
//        return getHtml(info, new HtmlLoader() {
//            @Override
//            public void notifyRetry(int retry, int delay, Throwable e) {
//            }
//
//            @Override
//            public void notifyDownloading() {
//            }
//
//            @Override
//            public void notifyMoved() {
//            }
//        }, new AtomicBoolean(false));
//    }
//
//    public static String getHtml(final URL source, final HtmlLoader load, final AtomicBoolean stop) {
//        return getHtml(new DownloadInfo(source), load, stop);
//    }
//
//    public static String getHtml(final DownloadInfo source, final HtmlLoader load, final AtomicBoolean stop) {
//        String html = RetryWrap.wrap(stop, new RetryWrap.WrapReturn<String>() {
//            DownloadInfo info = source;
//
//            @Override
//            public void proxy() {
//                info.getProxy().set();
//            }
//
//            @Override
//            public void resume() {
//                info.setRetry(0);
//            }
//
//            @Override
//            public void error(Throwable e) {
//                info.setRetry(info.getRetry() + 1);
//            }
//
//            @Override
//            public boolean retry(int delay, Throwable e) {
//                load.notifyRetry(info.getRetry(), delay, e);
//                return RetryWrap.retry(info.getRetry());
//            }
//
//            @Override
//            public String download() throws IOException {
//                HttpURLConnection conn = info.openConnection();
//                RetryWrap.check(conn);
//                return getHtml(conn, stop);
//            }
//
//            @Override
//            public void moved(URL url) {
//                DownloadInfo old = info;
//                info = new DownloadInfo(url);
//                info.setReferer(old.getReferer());
//                load.notifyMoved();
//            }
//        });
//        return html;
//    }
//
//    public static String getHtml(HttpURLConnection conn, AtomicBoolean stop) throws IOException {
//        InputStream is = conn.getInputStream();
//
//        String enc = conn.getContentEncoding();
//
//        if (enc == null) {
//            Pattern p = Pattern.compile("charset=(.*)");
//            Matcher m = p.matcher(conn.getHeaderField("Content-Type"));
//            if (m.find()) {
//                enc = m.group(1);
//            }
//        }
//
//        if (enc == null)
//            enc = UTF8;
//
//        BufferedReader br = new BufferedReader(new InputStreamReader(is, enc));
//
//        String line = null;
//
//        StringBuilder contents = new StringBuilder();
//        while ((line = br.readLine()) != null) {
//            contents.append(line);
//            contents.append("\n");
//            if (stop.get())
//                throw new DownloadInterruptedError("stop");
//            if (Thread.currentThread().isInterrupted())
//                throw new DownloadInterruptedError("interrupted");
//        }
//        return contents.toString();
//    }
}
