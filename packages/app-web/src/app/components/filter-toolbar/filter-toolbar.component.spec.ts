import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { FilterToolbarComponent } from './filter-toolbar.component';
import { FilterToolbarModule } from './filter-toolbar.module';

describe('NotificationBubbleComponent', () => {
  let component: FilterToolbarComponent<any>;
  let fixture: ComponentFixture<FilterToolbarComponent<any>>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [FilterToolbarModule],
    }).compileComponents();

    fixture = TestBed.createComponent(FilterToolbarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
