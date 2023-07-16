package cn.lee.hotel.service.impl;

import cn.lee.hotel.mapper.HotelMapper;
import cn.lee.hotel.pojo.Hotel;
import cn.lee.hotel.pojo.HotelDoc;
import cn.lee.hotel.pojo.PageResult;
import cn.lee.hotel.pojo.RequestParams;
import cn.lee.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {

    @Autowired
    private RestHighLevelClient client;

    @Override
    public PageResult search(RequestParams params) {
        SearchRequest request = new SearchRequest("hotel");

        //must
        basicQuery(params, request);

        //分页
        request.source().from((params.getPage() - 1) * params.getSize()).size(params.getSize());
        //排序
        if (!params.getSortBy().equals("default")){
            request.source().sort(params.getSortBy(), SortOrder.DESC);
        }
        //地址排序
        String location = params.getLocation();
        if (location != null && !location.equals("")){
            request.source().sort(SortBuilders.geoDistanceSort(
                    "location", new GeoPoint(location)
            ).order(SortOrder.ASC).unit(DistanceUnit.KILOMETERS));
        }

        try {
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            return handleResponse(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void basicQuery(RequestParams params, SearchRequest request) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        String key = params.getKey();
        if (key == null || "".equals(key)){
            boolQuery.must(QueryBuilders.matchAllQuery());
        }else {
            boolQuery.must(QueryBuilders.matchQuery("all", params.getKey()));
        }
        //条件
        if (params.getCity() != null && !params.getCity().equals("")){
            boolQuery.filter(QueryBuilders.termQuery("city", params.getCity()));
        }
        if (params.getBrand() != null && !params.getBrand().equals("")){
            boolQuery.filter(QueryBuilders.termQuery("brand", params.getBrand()));
        }
        if (params.getStarName() != null && !params.getStarName().equals("")){
            boolQuery.filter(QueryBuilders.termQuery("starName", params.getStarName()));
        }
        if (params.getMinPrice() != null && params.getMaxPrice() != null){
            boolQuery.filter(QueryBuilders.rangeQuery("price")
                    .gte(params.getMinPrice()).lte(params.getMaxPrice()));
        }
        //function score
        FunctionScoreQueryBuilder functionScoreQuery =
                QueryBuilders.functionScoreQuery(
                        //原始查询。相关性算分的查询
                        boolQuery,
                        //function score数组
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                                //具体的一个function score
                                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                        //过滤条件
                                        QueryBuilders.matchQuery("isAD", true),
                                        //算分函数
                                        ScoreFunctionBuilders.weightFactorFunction(10)
                                )
                        });
        request.source().query(functionScoreQuery);
    }

    @Override
    public Map<String, List<String>> filters(RequestParams params) {
        SearchRequest request = new SearchRequest("hotel");
        basicQuery(params, request);
        request.source().size(0);
        request.source().aggregation(AggregationBuilders
                .terms("cityAgg")
                .field("city")
                .size(100)
        );
        request.source().aggregation(AggregationBuilders
                .terms("brandAgg")
                .field("brand")
                .size(100)
        );
        request.source().aggregation(AggregationBuilders
                .terms("starAgg")
                .field("starName")
                .size(100)
        );
        Map<String, List<String>> res = new HashMap<>();
        try {
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            Aggregations aggregations = response.getAggregations();
            List<String> cityList = getList(aggregations, "cityAgg");
            List<String> brandList = getList(aggregations, "brandAgg");
            List<String> starList = getList(aggregations, "starAgg");
            res.put("城市", cityList);
            res.put("品牌", brandList);
            res.put("星级", starList);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return res;
    }

    @Override
    public List<String> suggest(String key) {
        List<String> res = new ArrayList<>();
        SearchRequest request = new SearchRequest("hotel");
        request.source().suggest(new SuggestBuilder().addSuggestion(
                "suggestions",
                SuggestBuilders.completionSuggestion("suggestion")
                        .prefix(key)
                        .skipDuplicates(true)
                        .size(10)
        ));
        try {
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            CompletionSuggestion suggestion = response.getSuggest().getSuggestion("suggestions");
            for (CompletionSuggestion.Entry.Option option : suggestion.getOptions()) {
                res.add(option.getText().toString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return res;
    }

    private List<String> getList(Aggregations aggregations, String term){
        List<String> res = new ArrayList<>();
        Terms terms = aggregations.get(term);
        List<? extends Terms.Bucket> buckets = terms.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            res.add(bucket.getKeyAsString());
        }
        return res;
    }

    private PageResult handleResponse(SearchResponse response){
        SearchHits hits = response.getHits();
        long total = hits.getTotalHits().value;//得到的结果总数
        List<HotelDoc> list = new ArrayList<>();
        SearchHit[] searchHits = hits.getHits();//结果
        for (SearchHit searchHit : searchHits) {
            HotelDoc hotelDoc = JSON.parseObject(searchHit.getSourceAsString(), HotelDoc.class);
            Object[] sortValues = searchHit.getSortValues();
            if (sortValues != null && sortValues.length != 0){
                hotelDoc.setDistance(sortValues[0]);
            }
//            Map<String, HighlightField> highlightFields = searchHit.getHighlightFields();
//            if (!CollectionUtils.isEmpty(highlightFields)){
//                HighlightField field = highlightFields.get("name");
//                if (field != null){
//                    hotelDoc.setName(field.getFragments()[0].toString());
//                }
//            }
            list.add(hotelDoc);
        }
        return new PageResult(total, list);
    }
}
