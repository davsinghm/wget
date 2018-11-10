package com.davsinghm.wget.core;

public class WGet {

    /*private Direct direct;
    private File targetFile;
    private DownloadInfo info;

    public DownloadInfo getInfo() {
        return info;
    }

    public WGet(DownloadInfo info, File targetFile) {
        this.info = info;
        this.targetFile = targetFile;
        direct = createDirect();
        Logs.v("WGet", "New Download: " + info.getDInfoID() + "\nURL: " + info.getSource() + "\nFile: " + targetFile.getAbsolutePath());
    }

    public WGet(URL source, File target) {
        create(source, target);
    }

    private Direct createDirect() {
        if (info.isMultipart()) {
            Logs.v("WGet", "createDirect(): MultiPart");
            return new DirectMultipart(info, targetFile, info.getThreadCount());
        } else if (info.hasRange()) {
            Logs.v("WGet", "createDirect(): Range");
            return new DirectRange(info, targetFile);
        } else {
            Logs.v("WGet", "createDirect(): Single");
            return new DirectSingle(info, targetFile);
        }
    }

    public void download(AtomicBoolean stop, Runnable notify) {
        Logs.v("WGet", "download(): invoked");
        direct.download(stop, notify);
    }

    private void create(URL source, File target) {
        info = new DownloadInfo(source);
        info.extract();
        targetFile = calcName(info, target);
        direct = createDirect();
    }

    public void download() {
        download(new AtomicBoolean(false), new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    public static File calcName(URL source, File target) {
        DownloadInfo info = new DownloadInfo(source);
        info.extract();

        return calcName(info, target);
    }

    *//**
     * @param target 1. can point to directory - generate exclusive (1) name
     *               2. to exisiting file
     *               3. to non existing file
     *//*
    public static File calcName(DownloadInfo info, File target) {

        String name = info.getContentFilename();

        if (name == null)
            name = new File(info.getSource().getPath()).getName();

        try {
            name = URLDecoder.decode(name, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String nameNoExt = FilenameUtils.removeExtension(name);
        String ext = FilenameUtils.getExtension(name);

        File targetFile;

        if (target.isDirectory()) {
            targetFile = FileUtils.getFile(target, name);
            int i = 1;
            while (targetFile.exists()) {
                targetFile = FileUtils.getFile(target, nameNoExt + " (" + i + ")." + ext);
                i++;
            }
        } else {
            try {
                FileUtils.forceMkdir(new File(target.getParent()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            targetFile = target;
        }

        return targetFile;
    }

    public static String getHtml(URL source) {
        return getHtml(source, new HtmlLoader() {
            @Override
            public void notifyRetry(int delay, Throwable e) {
            }

            @Override
            public void notifyDownloading() {
            }

            @Override
            public void notifyMoved() {
            }
        }, new AtomicBoolean(false));
    }

    public static String getHtml(final URL source, final HtmlLoader load, final AtomicBoolean stop) {
        return getHtml(new DownloadInfo(source), load, stop);
    }

    public static String getHtml(final DownloadInfo source, final HtmlLoader load, final AtomicBoolean stop) {
        return RetryWrap.wrap(stop, new RetryWrap.Wrap<String>() {
            DownloadInfo info = source;

            @Override
            public void retry(int delay, Throwable e) {
                load.notifyRetry(delay, e);
            }

            @Override
            public String download() throws IOException {
                HttpURLConnection conn = (HttpURLConnection) info.getSource().openConnection();

                conn.setConnectTimeout(Constants.WGET_CONNECT_TIMEOUT);
                conn.setReadTimeout(Constants.WGET_READ_TIMEOUT);

                conn.setRequestProperty("User-Agent", info.getUserAgent());
                if (info.getReferrer() != null)
                    conn.setRequestProperty("Referer", info.getReferrer().toExternalForm());

                RetryWrap.checkResponse(conn);

                InputStream is = conn.getInputStream();

                String enc = conn.getContentEncoding();

                if (enc == null) {
                    Pattern p = Pattern.compile("charset=(.*)");
                    Matcher m = p.matcher(conn.getHeaderField("Content-Type"));
                    if (m.find())
                        enc = m.group(1);
                }

                if (enc == null)
                    enc = "UTF-8";

                BufferedReader br = new BufferedReader(new InputStreamReader(is, enc));

                String line;

                StringBuilder contents = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    contents.append(line);
                    contents.append("\n");

                    if (stop.get())
                        throw new DownloadInterruptedError("stop");
                }

                return contents.toString();
            }

            @Override
            public void moved(URL url) {
                DownloadInfo old = info;
                info = new DownloadInfo(url);
                info.setReferrer(old.getReferrer());

                load.notifyMoved();
            }

        });
    }

    public interface HtmlLoader {
        *//**
         * some socket problem, retyring
         *
         * @param delay
         * @param e
         *//*
        void notifyRetry(int delay, Throwable e);

        *//**
         * start downloading
         *//*
        void notifyDownloading();

        *//**
         * document moved, relocating
         *//*
        void notifyMoved();
    }*/
}
