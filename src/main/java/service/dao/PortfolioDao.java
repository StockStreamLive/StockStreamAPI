package service.dao;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import service.dao.cache.PortfolioCache;
import stockstream.database.HistoricalEquityValue;
import stockstream.util.JSONUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class PortfolioDao {

    @Autowired
    private PortfolioCache portfolioCache;

    private static final String DATA_BUCKET_NAME = "api.stockstream.live";
    private static final String PORTFOLIO_FOLDER = "portfolio";

    private final AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                                                           .withRegion(Regions.US_EAST_1)
                                                           .withCredentials(new EnvironmentVariableCredentialsProvider())
                                                           .build();

    public String getCurrentPortfolio() throws IOException {
        final S3Object object = s3Client.getObject(new GetObjectRequest(DATA_BUCKET_NAME, PORTFOLIO_FOLDER + "/current"));
        final InputStream objectData = object.getObjectContent();

        final String portfolioData = IOUtils.toString(objectData, Charset.defaultCharset());

        objectData.close();

        return portfolioData;
    }

    public String getSnapshotsForDate(final String date) {
        final ObjectListing objectListing = s3Client.listObjects(DATA_BUCKET_NAME, PORTFOLIO_FOLDER + "/" + date);

        final List<String> keys = new ArrayList<>();

        objectListing.getObjectSummaries().forEach(s3ObjectSummary -> {
            final String basename = FilenameUtils.getName(s3ObjectSummary.getKey());
            keys.add(basename);
        });

        return JSONUtil.serializeObject(keys).orElse("[]");
    }

    public String getSnapshotFileForDate(final String date, final String filename) throws IOException {
        final S3Object object = s3Client.getObject(new GetObjectRequest(DATA_BUCKET_NAME, PORTFOLIO_FOLDER + "/" + date + "/" + filename));
        final InputStream objectData = object.getObjectContent();

        final String portfolioData = IOUtils.toString(objectData, Charset.defaultCharset());

        objectData.close();

        return portfolioData;
    }

    public List<HistoricalEquityValue> getPortfolioValuesForDate(final String date) {
        return portfolioCache.getIntradayPortfolioValues().get(date);
    }

    public List<HistoricalEquityValue> getPortfolioValues() {
        return portfolioCache.getDailyPortfolioValues();
    }

}
