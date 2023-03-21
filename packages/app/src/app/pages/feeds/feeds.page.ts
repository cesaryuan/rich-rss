import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BasicNativeFeed, FeedService } from '../../services/feed.service';
import { Filters, FilterData, FilterOption } from '../../components/filter-toolbar/filter-toolbar.component';
import { FilteredList } from '../../components/filtered-list';
import { ActionSheetButton, ActionSheetController } from '@ionic/angular';
import { Pagination } from 'src/app/services/pagination.service';
import { FormControl } from '@angular/forms';
import {
  GqlContentCategoryTag,
  GqlContentSortTag,
  GqlHealth,
  GqlOrderByInput,
  GqlSortOrder,
  GqlVisibility
} from '../../../generated/graphql';
import { FeedFilterValues } from '../buckets/buckets.page';

export const toOrderBy = (sortBy: GqlContentSortTag): GqlOrderByInput => {
  console.log(sortBy, GqlContentSortTag.Newest);
  switch (sortBy) {
    case GqlContentSortTag.Newest: return {
      createdAt: GqlSortOrder.Desc
    };
    case GqlContentSortTag.Oldest: return {
      createdAt: GqlSortOrder.Asc
    };
    case GqlContentSortTag.Title: return {
      title: GqlSortOrder.Desc
    };
  }
};

export const enumToMap = <T> (theEnum: T): FilterOption[] => Object.keys(theEnum)
  .map((name) => ({
    key: theEnum[name],
    value: name
  }));

@Component({
  selector: 'app-feeds-page',
  templateUrl: './feeds.page.html',
  styleUrls: ['./feeds.page.scss'],
})
export class FeedsPage extends FilteredList<BasicNativeFeed, FilterData<FeedFilterValues>> {
  filters: Filters<FeedFilterValues> = {
    tag: {
      name: 'tag',
      control: new FormControl<GqlContentCategoryTag[]>([]),
      options: enumToMap(GqlContentCategoryTag),
    },
    visibility: {
      name: 'visibility',
      control: new FormControl<GqlVisibility[]>([]),
      options: enumToMap(GqlVisibility),
    },
    health: {
      name: 'health',
      control: new FormControl<GqlHealth[]>([]),
      options: enumToMap(GqlHealth),
    },
  };

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly feedService: FeedService,
    readonly actionSheetCtrl: ActionSheetController
  ) {
    super('feed', actionSheetCtrl);
  }

  getBulkActionButtons(): ActionSheetButton<any>[] {
    return [];
  }
  async fetch(
    filterData: FilterData<FeedFilterValues>
  ): Promise<[BasicNativeFeed[], Pagination]> {
    const response = await this.feedService.searchNativeFeeds({
      where: {
        query: '',
        health: {
          oneOf: filterData.filters.health
        },
        visibility: {
          oneOf: filterData.filters.visibility
        }
      },
      orderBy: toOrderBy(filterData.sortBy),
      page: 0,
    });
    return [response.nativeFeeds, response.pagination];
  }

  getHost(url: string): string {
    return new URL(url).hostname;
  }
}
