package cn.lee.hotel;

import cn.lee.hotel.pojo.HotelDoc;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class HotelSearchTest {

    private RestHighLevelClient client;

    /**
     * 查询
     * @throws IOException
     */
    @Test
    void search() throws IOException {
        SearchRequest request = new SearchRequest("hotel");

//        request.source().query(QueryBuilders.matchAllQuery());    //查询所有
//        request.source().query(QueryBuilders.matchQuery("all", "如家"));//单字段查询
//        request.source().query(QueryBuilders.multiMatchQuery("如家","name", "brand"));//多字段查询
//        request.source().query(QueryBuilders.termQuery("city", "北京"));//词条查询
//        request.source().query(QueryBuilders.rangeQuery("price").gt(100).lt(400));//范围查询
        request.source().query(QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("city", "上海"))
                .filter(QueryBuilders.rangeQuery("price").lte(300)));//bool查询

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        SearchHits hits = response.getHits();
        long total = hits.getTotalHits().value;//得到的结果总数
        SearchHit[] searchHits = hits.getHits();//结果
        for (SearchHit searchHit : searchHits) {
            System.out.println(searchHit.getSourceAsString());
        }
        System.out.println(total);
    }

    /**
     * 排序和分页
     * @throws IOException
     */
    @Test
    void sortAndSize() throws IOException {
        SearchRequest request = new SearchRequest("hotel");

        request.source().from(0).size(3);   //分页
        request.source().sort("price", SortOrder.DESC);//排序

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        SearchHits hits = response.getHits();
        long total = hits.getTotalHits().value;//得到的结果总数
        SearchHit[] searchHits = hits.getHits();//结果
        for (SearchHit searchHit : searchHits) {
            System.out.println(searchHit.getSourceAsString());
        }
        System.out.println(total);
    }

    /**
     * 搜索高亮显示
     * @throws IOException
     */
    @Test
    void highLight() throws IOException {
        SearchRequest request = new SearchRequest("hotel");

        request.source().query(QueryBuilders.matchQuery("all", "皇冠"));
        request.source().highlighter(
                new HighlightBuilder().field("name").requireFieldMatch(false));//高亮

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        SearchHits hits = response.getHits();
        long total = hits.getTotalHits().value;//得到的结果总数
        SearchHit[] searchHits = hits.getHits();//结果
        for (SearchHit searchHit : searchHits) {
            HotelDoc hotelDoc = JSON.parseObject(searchHit.getSourceAsString(), HotelDoc.class);
            Map<String, HighlightField> highlightFields = searchHit.getHighlightFields();
            if (!CollectionUtils.isEmpty(highlightFields)){
                HighlightField field = highlightFields.get("name");
                if (field != null){
                    hotelDoc.setName(field.getFragments()[0].toString());
                }
            }
            System.out.println(hotelDoc);
        }
        System.out.println(total);
    }

    /**
     * 数据聚合
     */
    @Test
    void testAggregation() throws IOException {
        SearchRequest request = new SearchRequest("hotel");

        request.source().size(0);//不要文档，只需聚合的数据
        request.source().aggregation(AggregationBuilders
                .terms("brandAgg")
                .field("brand")
                .size(20)
        );

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        Aggregations aggregations = response.getAggregations();
        Terms brandAgg = aggregations.get("brandAgg");
        List<? extends Terms.Bucket> buckets = brandAgg.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            System.out.println(bucket.getKeyAsString() + "   " + bucket.getDocCount());
        }
    }

    /**
     * 自动补全
     */
    @Test
    void testSuggest() throws IOException {
        SearchRequest request = new SearchRequest("hotel");
        request.source().suggest(new SuggestBuilder().addSuggestion(
                "suggestions",
                SuggestBuilders.completionSuggestion("suggestion")
                        .prefix("hc")
                        .skipDuplicates(true)
                        .size(10)
        ));
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        CompletionSuggestion suggestion =  response.getSuggest().getSuggestion("suggestions");
        for (CompletionSuggestion.Entry.Option option : suggestion.getOptions()) {
            System.out.println(option.getText());
        }
    }

    @BeforeEach
    void beforeAll() {
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.41.134:9200")
        ));
    }

    @AfterEach
    void afterAll() throws IOException {
        this.client.close();
    }
}
