import { JabitServerPage } from './app.po';

describe('jabit-server App', () => {
  let page: JabitServerPage;

  beforeEach(() => {
    page = new JabitServerPage();
  });

  it('should display message saying app works', () => {
    page.navigateTo();
    expect(page.getParagraphText()).toEqual('app works!');
  });
});
